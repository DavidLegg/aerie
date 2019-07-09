/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { ElementRef, Type } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { MockElementRef } from '../mocks';
import { defaultEditorId } from '../reducers/file.reducer';
import { SeqEditorService } from './seq-editor.service';

describe('SeqEditorService', () => {
  let elementRef: ElementRef;
  let seqEditorService: SeqEditorService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [],
      providers: [
        SeqEditorService,
        { provide: ElementRef, useClass: MockElementRef },
      ],
    });

    elementRef = TestBed.get(ElementRef as Type<ElementRef>);
    seqEditorService = TestBed.get(SeqEditorService);
  });

  it('should be created', () => {
    expect(seqEditorService).toBeDefined();
  });

  it('the editor instance should initially be null', () => {
    expect(seqEditorService.editors).toBeNull();
  });

  it('the editor instance should be initialized after calling setEditor', () => {
    seqEditorService.setEditor(elementRef, defaultEditorId);
    expect(seqEditorService.editors).toBeDefined();
  });

  it('the editor instance should have new text after calling addText', () => {
    seqEditorService.setEditor(elementRef, defaultEditorId);
    const editor = seqEditorService.getEditor(
      defaultEditorId,
    ) as CodeMirror.Editor;

    const text = 'racecar';
    expect(editor.getValue()).toEqual('');
    seqEditorService.addText(text, defaultEditorId);
    expect(editor.getValue()).toEqual(text);
  });
});