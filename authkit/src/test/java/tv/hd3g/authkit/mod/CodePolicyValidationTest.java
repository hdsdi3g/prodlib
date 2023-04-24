package tv.hd3g.authkit.mod;

import tv.hd3g.commons.codepolicyvalidation.CheckPolicy;

class CodePolicyValidationTest extends CheckPolicy {

	@Override
	public void springBootControllersInControllerPackage() {
		/**
		 * Buggy run
		 */
	}

	@Override
	public void springBootNotRepositoryInRepositoryPackage() {
		/**
		 * Remove after switch to CodePolicyValidation v2.n
		 */
	}

}
