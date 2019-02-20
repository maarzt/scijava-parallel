// TODO: Add copyright stuff

package org.scijava.parallel;

import java.util.List;

import org.scijava.plugin.SingletonService;
import org.scijava.prefs.PrefService;
import org.scijava.service.SciJavaService;

/**
 * A service providing parallelization capabilities
 *
 * @author TODO: Add authors
 */
public interface ParallelService extends
	SingletonService<ParallelizationParadigm>, SciJavaService
{

	// TODO: This API is super-preliminary

	/**
	 * Returns an instance of the parallelization paradigm corresponding to the
	 * chosen profile, if available
	 * 
	 * @return Instance of the corresponding parallelization paradigm
	 */
	public ParallelizationParadigm getParadigm();

	// TODO: This method is meant to be package-specific only,
	// profiles should be accessible only from the prospective configuration
	// plugin
	/**
	 * Returns all saved parallelization paradigm profiles
	 * 
	 * @return List of {@link ParallelizationParadigmProfile}
	 */
	public List<ParallelizationParadigmProfile> getProfiles();

	// TODO: This method is meant to be package-specific only,
	// profiles should be accessible only from the prospective configuration
	// plugin
	/**
	 * Saves the given {@link ParallelizationParadigmProfile} using the
	 * {@link PrefService}
	 */
	public void addProfile(final ParallelizationParadigmProfile profile);

	// TODO: This method is meant to be package-specific only,
	// profiles should be accessible only from the prospective configuration
	// plugin
	/**
	 * Selects the given {@link ParallelizationParadigmProfile}
	 * 
	 * @param name of the {@link ParallelizationParadigmProfile} to be selected
	 */
	public void selectProfile(final String name);

	// TODO: This method is meant to be package-specific only,
	// profiles should be accessible only from the prospective configuration
	// plugin
	/** Removes all saved parallelization paradigm profiles */
	public void deleteProfiles();

	// -- PTService methods --

	@Override
	default Class<ParallelizationParadigm> getPluginType() {
		return ParallelizationParadigm.class;
	}
}
