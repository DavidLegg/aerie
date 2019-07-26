/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material';

@Component({
  selector: 'raven-load-epoch-dialog',
  styleUrls: ['./raven-load-epoch-dialog.component.css'],
  templateUrl: './raven-load-epoch-dialog.component.html',
})
export class RavenLoadEpochDialogComponent {
  sourceUrl: string;

  constructor(
    public dialogRef: MatDialogRef<RavenLoadEpochDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any,
  ) {
    this.sourceUrl = data.sourceUrl;
  }

  /**
   * Cancels and closes the dialog.
   */
  onCancel() {
    this.dialogRef.close();
  }

  onAppendAndReplaceExistingEpochs() {
    this.dialogRef.close({
      replaceAction: 'AppendAndReplace',
      sourceUrl: this.sourceUrl,
    });
  }

  onRemoveExistingEpochs() {
    this.dialogRef.close({
      replaceAction: 'RemoveAll',
      sourceUrl: this.sourceUrl,
    });
  }
}