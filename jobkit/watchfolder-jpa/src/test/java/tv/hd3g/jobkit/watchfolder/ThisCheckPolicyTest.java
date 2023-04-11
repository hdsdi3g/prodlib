package tv.hd3g.jobkit.watchfolder;

import org.junit.jupiter.api.Test;

import tv.hd3g.commons.codepolicyvalidation.CheckPolicy;

public class ThisCheckPolicyTest extends CheckPolicy {

	@Override
	@Test
	public void springBootNotRepositoryInRepositoryPackage() {// NOSONAR S2699
		/**
		 * Disabled (see https://github.com/hdsdi3g/codepolicyvalidation/issues/37)
		 */
	}

}
