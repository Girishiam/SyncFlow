import { APP_INITIALIZER, ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideIonicAngular } from '@ionic/angular';

import { routes } from './app.routes';
import { jwtInterceptor } from './core/interceptors/jwt.interceptor';
import { FeatureFlagsService } from './core/services/feature-flags.service';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes, withComponentInputBinding()),
    provideHttpClient(withInterceptors([jwtInterceptor])),
    provideIonicAngular({ mode: 'md' }),
    {
      provide: APP_INITIALIZER,
      multi: true,
      deps: [FeatureFlagsService],
      useFactory: (features: FeatureFlagsService) => () => features.load()
    }
  ]
};
