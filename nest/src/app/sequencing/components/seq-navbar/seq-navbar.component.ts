/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  Input,
  Output,
} from '@angular/core';
import { Editor, SequenceTab } from '../../models';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'seq-navbar',
  styleUrls: ['./seq-navbar.component.css'],
  templateUrl: './seq-navbar.component.html',
})
export class SeqNavbarComponent {
  @Input()
  openedTabs: SequenceTab[] | null;

  @Input()
  editor: Editor;

  @Output()
  createTab: EventEmitter<any> = new EventEmitter<any>();

  @Output()
  closeTab: EventEmitter<any> = new EventEmitter<any>();

  @Output()
  switchTab: EventEmitter<any> = new EventEmitter<any>();
}