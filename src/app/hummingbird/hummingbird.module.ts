/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { HttpClientModule } from '@angular/common/http';
import { NgModule } from '@angular/core';
import { EffectsModule } from '@ngrx/effects';
import { StoreModule } from '@ngrx/store';
import { HummingbirdAppModule } from './containers/hummingbird-app/hummingbird-app.module';
import { CommandDictionaryEffects } from './effects/command-dictionary.effect';
import { HummingbirdRoutingModule } from './hummingbird-routing.module';
import { reducers } from './hummingbird-store';

@NgModule({
  imports: [
    HttpClientModule,
    HummingbirdRoutingModule,
    StoreModule.forFeature('hummingbird', reducers),
    EffectsModule.forFeature([
      CommandDictionaryEffects,
    ]),
    HummingbirdAppModule,
  ],
})
export class HummingbirdModule {}
