/*
 * This file is part of jobkit-watchfolder.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * Copyright (C) hdsdi3g for hd3g.tv 2022
 *
 */
package tv.hd3g.jobkit.watchfolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.mockito.internal.verification.VerificationModeFactory.only;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static tv.hd3g.jobkit.engine.SupervisableResultState.WORKS_DONE;
import static tv.hd3g.jobkit.watchfolder.FolderActivity.OBSERVEDFOLDER;
import static tv.hd3g.jobkit.watchfolder.RetryScanPolicyOnUserError.RETRY_FOUNDED_FILE;
import static tv.hd3g.jobkit.watchfolder.WatchFolderPickupType.FILES_ONLY;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import net.datafaker.Faker;
import tv.hd3g.jobkit.engine.FlatJobKitEngine;
import tv.hd3g.jobkit.engine.Job;
import tv.hd3g.jobkit.engine.RunnableWithException;
import tv.hd3g.jobkit.engine.SupervisableEndEvent;

class FolderActivityTest {

	static Faker faker = Faker.instance();

	FolderActivity a;
	FlatJobKitEngine jobKit;
	ObservedFolder observedFolder;
	SupervisableEndEvent event;

	@Mock
	Exception exception;

	@BeforeEach
	void init() throws Exception {
		openMocks(this).close();
		// Mockito.doThrow(new Exception()).when();
		jobKit = new FlatJobKitEngine();
		observedFolder = new ObservedFolder();
		a = (observedFolder, scanTime, scanResult) -> {
		};
	}

	@AfterEach
	void end() {
		verifyNoMoreInteractions(exception);
	}

	@Test
	void testOnStartScan() {
		run(() -> a.onStartScan(observedFolder));

		event = jobKit.getEndEventsList().get(0);
		assertTrue(event.isInternalStateChangeMarked());
		assertEquals(WORKS_DONE, event.result().state());
		assertEquals(OBSERVEDFOLDER, event.typeName());
		assertTrue(FolderActivity.isFolderActivityEvent(event));
	}

	@Test
	void testOnStopScan() {
		run(() -> a.onStopScan(observedFolder));

		event = jobKit.getEndEventsList().get(0);
		assertTrue(event.isInternalStateChangeMarked());
		assertEquals(WORKS_DONE, event.result().state());
		assertEquals(OBSERVEDFOLDER, event.typeName());
		assertTrue(FolderActivity.isFolderActivityEvent(event));
	}

	@Test
	void testOnBeforeScan() {
		run(() -> a.onBeforeScan(observedFolder));
		assertTrue(jobKit.getEndEventsList().isEmpty());
	}

	@Test
	void testGetPickUpType() {
		assertEquals(FILES_ONLY, a.getPickUpType(null));
	}

	@Test
	void testOnScanErrorFolder() throws IOException {
		run(() -> a.onScanErrorFolder(observedFolder, exception));

		event = jobKit.getEndEventsList().get(0);
		assertTrue(event.isInternalStateChangeMarked());
		assertEquals(exception, event.error());
		assertEquals(OBSERVEDFOLDER, event.typeName());
		assertTrue(FolderActivity.isFolderActivityEvent(event));
	}

	@Test
	void testRetryScanPolicyOnUserError() {
		assertEquals(RETRY_FOUNDED_FILE, a.retryScanPolicyOnUserError(null, null, null));
	}

	@Test
	void testIsFolderActivityEvent_nopeIsInternalStateChangeMarked() {
		event = Mockito.mock(SupervisableEndEvent.class);
		when(event.isInternalStateChangeMarked()).thenReturn(false);
		assertFalse(FolderActivity.isFolderActivityEvent(event));
		verify(event, only()).isInternalStateChangeMarked();
	}

	@Test
	void testIsFolderActivityEvent_badType() {
		event = Mockito.mock(SupervisableEndEvent.class);
		when(event.isInternalStateChangeMarked()).thenReturn(true);
		when(event.typeName()).thenReturn(faker.numerify("type###"));

		assertFalse(FolderActivity.isFolderActivityEvent(event));

		verify(event, times(1)).isInternalStateChangeMarked();
		verify(event, times(1)).typeName();
		verifyNoMoreInteractions(event);
	}

	void run(final RunnableWithException r) {
		jobKit.runOneShot(new Job() {

			@Override
			public void run() throws Exception {
				r.run();
			}

			@Override
			public String getJobSpoolname() {
				return "testSpool";
			}

			@Override
			public String getJobName() {
				return "TestJob";
			}
		});
	}

}
