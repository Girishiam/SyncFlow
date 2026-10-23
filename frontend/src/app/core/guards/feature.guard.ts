import { CanMatchFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { FeatureFlagsService, FeatureName } from '../services/feature-flags.service';

/** Lets a route match only while the feature is on; otherwise sends the user to the board. */
export const featureGuard = (feature: FeatureName): CanMatchFn => () =>
  inject(FeatureFlagsService).isEnabled(feature) || inject(Router).createUrlTree(['/dashboard/board']);
