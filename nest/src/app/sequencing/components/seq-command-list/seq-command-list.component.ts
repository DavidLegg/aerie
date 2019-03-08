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
import { MpsCommand } from '../../../../../../schemas';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'seq-command-list',
  styleUrls: ['./seq-command-list.component.css'],
  templateUrl: './seq-command-list.component.html',
})
export class SeqCommandListComponent {
  @Input()
  commands: MpsCommand[] | null;

  @Output()
  selectCommand: EventEmitter<string> = new EventEmitter<string>();

  onSelectCommand(command: MpsCommand) {
    this.selectCommand.emit(command.name);
  }
}
