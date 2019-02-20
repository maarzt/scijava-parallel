// TODO: Add copyright stuff

package org.scijava.parallel;

import java.io.Serializable;

/**
 * A ParallelizationParadigmProfile object encapsulates user-specific
 * information which is used with a given {@link ParallelizationParadigm}. This
 * would typically include user name, password, host address or port number.
 *
 * @author Petr Bainar
 */
public class ParallelizationParadigmProfile implements Serializable {

	private static final long serialVersionUID = 1L;

	/** Profile name */
	private final String profileName;

	/** The {@link ParallelizationParadigm} type to be used in this profile */
	private final Class<? extends ParallelizationParadigm> paradigmType;

	/** A flag determining whether this profile has been selected by the user */
	private Boolean selected;

	/**
	 * Returns {@link #profileName}
	 */
	String getName() {
		return profileName;
	}

	/**
	 * Gets the {@link ParallelizationParadigm} type which is to be used in this
	 * profile
	 * 
	 * @return {@link Class} of given {@link ParallelizationParadigm} 
	 */
	@SuppressWarnings("unchecked")
	<T extends ParallelizationParadigm> Class<T> getParadigmType() {
		if (ParallelizationParadigm.class.isAssignableFrom(paradigmType)) {
			return (Class<T>) paradigmType;
		}
		return null;
	}

	/**
	 * Returns the {@link #selected} flag
	 */
	Boolean isSelected() {
		return selected;
	}

	/**
	 * Sets the {@link #selected} flag
	 */
	void setSelected(final Boolean selected) {
		this.selected = selected;
	}

	public ParallelizationParadigmProfile(
		final Class<? extends ParallelizationParadigm> paradigmType,
		final String profileName)
	{
		this.paradigmType = paradigmType;
		this.profileName = profileName;
	}

}
