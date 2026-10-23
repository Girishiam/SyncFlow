import { Injectable, signal } from '@angular/core';

/** Optional sections of the app that can be switched on or off in public/app-config.json. */
export type FeatureName = 'budget';

/** Used when app-config.json is missing or leaves a feature out. */
const DEFAULTS: Record<FeatureName, boolean> = {
  budget: false
};

/**
 * Reads public/app-config.json once at startup. Because the file is fetched at runtime,
 * a deployed build can be reconfigured by editing it, without rebuilding the app.
 */
@Injectable({ providedIn: 'root' })
export class FeatureFlagsService {
  private readonly flags = signal<Record<FeatureName, boolean>>({ ...DEFAULTS });

  async load(): Promise<void> {
    try {
      const res = await fetch('app-config.json', { cache: 'no-store' });
      if (!res.ok) return;
      const config = await res.json();
      this.flags.set({ ...DEFAULTS, ...(config?.features ?? {}) });
    } catch {
      // Keep the defaults; a missing or malformed file must not stop the app from starting.
    }
  }

  isEnabled(feature: FeatureName): boolean {
    return this.flags()[feature] === true;
  }
}
