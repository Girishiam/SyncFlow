import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { of } from 'rxjs';
import { TeamFacade } from './team.facade';
import { BoardFacade } from '../../board/application/board.facade';
import { ProjectService } from '../../../core/services/project.service';
import { AuthService } from '../../../core/services/auth.service';
import { BankUser, Project } from '../../../core/models/project.models';

const user = (id: string, fullName: string): BankUser =>
  ({ id, fullName, email: `${id}@bank.test`, status: 'ACTIVE', onLeave: false, role: { id: 1, name: 'DEVELOPER' } });

describe('TeamFacade', () => {
  let projectService: jasmine.SpyObj<ProjectService>;
  let board: { projects: ReturnType<typeof signal<Project[]>>; selectedProjectId: ReturnType<typeof signal<number>>;
               bankUsers: ReturnType<typeof signal<BankUser[]>>; selectProject: jasmine.Spy; loadBoard: jasmine.Spy };

  beforeEach(() => {
    projectService = jasmine.createSpyObj<ProjectService>('ProjectService', ['getMembers', 'createProject', 'getAllProjects']);
    projectService.getMembers.and.returnValue(of([
      { userId: 'me', fullName: 'Tariq Manager', email: 'me@bank.test', avatar: 'TM', role: 'MANAGER', openPoints: 0, openTasks: 0 }
    ]));
    board = {
      projects: signal<Project[]>([{ id: 1, name: 'Payments', status: 'ACTIVE', memberCount: 1, canManageTeam: true }]),
      selectedProjectId: signal(1),
      bankUsers: signal<BankUser[]>([user('me', 'Tariq Manager'), user('dev', 'Farhan Dev'), user('sqa', 'Sarah SQA')]),
      selectProject: jasmine.createSpy('selectProject'),
      loadBoard: jasmine.createSpy('loadBoard')
    };
    TestBed.configureTestingModule({
      providers: [
        { provide: ProjectService, useValue: projectService },
        { provide: BoardFacade, useValue: board },
        { provide: AuthService, useValue: { currentUserSignal: signal({ id: 'me' }) } }
      ]
    });
  });

  it('offers only people who are not on the team yet', () => {
    const team = TestBed.inject(TeamFacade);
    TestBed.flushEffects();
    expect(team.members().map(m => m.userId)).toEqual(['me']);
    expect(team.candidates().map(u => u.id)).toEqual(['dev', 'sqa']);
    team.search.set('sar');
    expect(team.candidates().map(u => u.id)).toEqual(['sqa']);
  });

  it('needs a name and a date that is not in the past before going to step two', () => {
    const team = TestBed.inject(TeamFacade);
    team.openWizard();
    expect(team.draftDetailsValid()).toBeFalse();
    team.draftName.set('Card issuing');
    team.draftDueDate.set('2000-01-01');
    expect(team.draftDetailsValid()).toBeFalse();
    team.draftDueDate.set('');
    team.nextStep();
    expect(team.wizardStep()).toBe(2);
  });

  it('creates the project with the people picked, leaving the creator out of the list', () => {
    const team = TestBed.inject(TeamFacade);
    projectService.createProject.and.returnValue(of({ id: 9, name: 'Card issuing', status: 'ACTIVE' }));
    team.openWizard();
    expect(team.draftCandidates().map(u => u.id)).toEqual(['dev', 'sqa']);
    team.draftName.set('  Card issuing ');
    team.toggleDraftMember('dev');
    team.toggleDraftMember('sqa');
    team.setDraftRole('sqa', 'LEAD');
    team.createProject();

    const request = projectService.createProject.calls.mostRecent().args[0];
    expect(request.name).toBe('Card issuing');
    expect(request.members).toEqual([{ userId: 'dev', role: 'MEMBER' }, { userId: 'sqa', role: 'LEAD' }]);
    expect(board.selectProject).toHaveBeenCalledWith(9);
    expect(team.isWizardOpen()).toBeFalse();
  });
});
