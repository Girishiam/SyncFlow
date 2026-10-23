import { Injectable, computed, effect, inject, signal, untracked } from '@angular/core';
import { ProjectService } from '../../../core/services/project.service';
import { ErrorNotifierService } from '../../../core/services/error-notifier.service';
import { AuthService } from '../../../core/services/auth.service';
import { BankUser, ProjectMember, ProjectRole } from '../../../core/models/project.models';
import { BoardFacade } from '../../board/application/board.facade';

export const PROJECT_ROLES: { value: ProjectRole; label: string; hint: string }[] = [
  { value: 'MANAGER', label: 'Manager', hint: 'Runs the project and its team' },
  { value: 'LEAD', label: 'Lead', hint: 'Plans the work, can manage the team' },
  { value: 'MEMBER', label: 'Member', hint: 'Works on tasks' }
];

/**
 * The people on the selected project, and the two-step "New project" flow (details, then team).
 * Only team members can see a project or be assigned its tasks.
 */
@Injectable({ providedIn: 'root' })
export class TeamFacade {
  private readonly projectService = inject(ProjectService);
  private readonly notifier = inject(ErrorNotifierService);
  private readonly auth = inject(AuthService);
  private readonly board = inject(BoardFacade);

  readonly members = signal<ProjectMember[]>([]);
  readonly isLoading = signal(false);
  readonly isPanelOpen = signal(false);
  readonly search = signal('');
  readonly newMemberRole = signal<ProjectRole>('MEMBER');

  readonly currentProject = computed(() =>
    this.board.projects().find(p => p.id === this.board.selectedProjectId()) ?? null);
  readonly canManageTeam = computed(() => !!this.currentProject()?.canManageTeam);

  /** People who can be assigned tasks in this project. */
  readonly assignable = computed(() =>
    this.members().map(m => ({ id: m.userId, fullName: m.fullName, avatar: m.avatar, openPoints: m.openPoints, onLeave: !!m.onLeave })));

  /** Active users who are not on the team yet, filtered by the search box. */
  readonly candidates = computed(() => {
    const onTeam = new Set(this.members().map(m => m.userId));
    const q = this.search().trim().toLowerCase();
    return this.board.bankUsers()
      .filter(u => u.status === 'ACTIVE' && !onTeam.has(u.id))
      .filter(u => !q || u.fullName.toLowerCase().includes(q) || u.email.toLowerCase().includes(q))
      .slice(0, 8);
  });

  constructor() {
    // Reload the team whenever another project is selected.
    effect(() => {
      const projectId = this.board.selectedProjectId();
      const known = this.board.projects().some(p => p.id === projectId);
      if (known) untracked(() => this.load(projectId));
    });
  }

  load(projectId = this.board.selectedProjectId()): void {
    if (!projectId) return;
    this.isLoading.set(true);
    this.projectService.getMembers(projectId).subscribe({
      next: members => { this.members.set(members); this.isLoading.set(false); },
      error: err => { this.isLoading.set(false); this.notifier.showClientError(err); }
    });
  }

  openPanel(): void {
    this.search.set('');
    this.newMemberRole.set('MEMBER');
    this.isPanelOpen.set(true);
    this.load();
  }

  closePanel(): void {
    this.isPanelOpen.set(false);
  }

  addMember(user: BankUser): void {
    const projectId = this.board.selectedProjectId();
    this.projectService.addMember(projectId, user.id, this.newMemberRole()).subscribe({
      next: () => {
        this.search.set('');
        this.load(projectId);
        this.refreshProjectCounts();
        this.notifier.showInfo(`${user.fullName} was added to the team.`);
      },
      error: err => this.notifier.showClientError(err)
    });
  }

  changeRole(member: ProjectMember, role: ProjectRole): void {
    if (member.role === role) return;
    const projectId = this.board.selectedProjectId();
    this.projectService.updateMemberRole(projectId, member.userId, role).subscribe({
      next: updated => this.members.update(list => list.map(m => m.userId === member.userId ? { ...m, role: updated.role } : m)),
      error: err => { this.notifier.showClientError(err); this.load(projectId); }
    });
  }

  removeMember(member: ProjectMember): void {
    const open = member.openTasks;
    const note = open > 0 ? `\n\nTheir ${open} open task${open === 1 ? '' : 's'} in this project will become unassigned.` : '';
    if (!confirm(`Remove ${member.fullName} from the team?${note}`)) return;
    const projectId = this.board.selectedProjectId();
    this.projectService.removeMember(projectId, member.userId).subscribe({
      next: res => {
        this.members.update(list => list.filter(m => m.userId !== member.userId));
        this.refreshProjectCounts();
        if (res.unassignedTasks > 0) this.board.loadBoard(projectId);
        this.notifier.showInfo(`${member.fullName} was removed from the team.`);
      },
      error: err => this.notifier.showClientError(err)
    });
  }

  isMe(member: ProjectMember): boolean {
    return member.userId === this.auth.currentUserSignal()?.id;
  }

  /** Keeps the project list (member counts, my role) in step after team changes. */
  private refreshProjectCounts(): void {
    this.projectService.getAllProjects().subscribe({
      next: projects => this.board.projects.set(projects),
      error: () => { /* the counts are cosmetic; the team list is already up to date */ }
    });
  }

  // ---------------------------------------------------------------- New project (two steps)

  readonly isWizardOpen = signal(false);
  readonly wizardStep = signal<1 | 2>(1);
  readonly isCreating = signal(false);
  readonly draftName = signal('');
  readonly draftDescription = signal('');
  readonly draftDueDate = signal('');
  readonly draftBudget = signal<number | null>(null);
  readonly draftCurrency = signal('USD');
  readonly draftSearch = signal('');
  /** userId -> role for everyone picked in step two (the creator is added as manager automatically). */
  readonly draftTeam = signal<Map<string, ProjectRole>>(new Map());

  readonly today = new Date().toISOString().slice(0, 10);

  readonly draftCandidates = computed(() => {
    const me = this.auth.currentUserSignal()?.id;
    const q = this.draftSearch().trim().toLowerCase();
    return this.board.bankUsers()
      .filter(u => u.status === 'ACTIVE' && u.id !== me)
      .filter(u => !q || u.fullName.toLowerCase().includes(q) || u.email.toLowerCase().includes(q) || (u.role?.name ?? '').toLowerCase().includes(q));
  });

  readonly draftDetailsValid = computed(() =>
    this.draftName().trim().length > 0 && (!this.draftDueDate() || this.draftDueDate() >= this.today));

  openWizard(): void {
    this.wizardStep.set(1);
    this.draftName.set('');
    this.draftDescription.set('');
    this.draftDueDate.set('');
    this.draftBudget.set(null);
    this.draftCurrency.set('USD');
    this.draftSearch.set('');
    this.draftTeam.set(new Map());
    this.isWizardOpen.set(true);
  }

  closeWizard(): void {
    if (this.isCreating()) return;
    this.isWizardOpen.set(false);
  }

  nextStep(): void {
    if (this.draftDetailsValid()) this.wizardStep.set(2);
  }

  toggleDraftMember(userId: string): void {
    this.draftTeam.update(team => {
      const next = new Map(team);
      if (next.has(userId)) next.delete(userId); else next.set(userId, 'MEMBER');
      return next;
    });
  }

  setDraftRole(userId: string, role: ProjectRole): void {
    this.draftTeam.update(team => new Map(team).set(userId, role));
  }

  createProject(): void {
    if (!this.draftDetailsValid() || this.isCreating()) return;
    this.isCreating.set(true);
    this.projectService.createProject({
      name: this.draftName().trim(),
      description: this.draftDescription().trim() || undefined,
      dueDate: this.draftDueDate() || null,
      totalBudget: this.draftBudget(),
      currency: this.draftCurrency(),
      members: [...this.draftTeam()].map(([userId, role]) => ({ userId, role }))
    }).subscribe({
      next: project => {
        this.isCreating.set(false);
        this.isWizardOpen.set(false);
        this.board.projects.update(list => [...list, project]);
        this.board.selectProject(project.id);
        this.notifier.showInfo(`${project.name} is ready.`);
      },
      error: err => { this.isCreating.set(false); this.notifier.showClientError(err); }
    });
  }
}
