// TODO: Add copyright stuff

package org.scijava.parallel;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.scijava.plugin.AbstractSingletonService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.prefs.PrefService;
import org.scijava.service.Service;

// TODO: Add description

@Plugin(type = Service.class)
public class DefaultParallelService extends AbstractSingletonService<ParallelizationParadigm>
		implements ParallelService {
	
	@Parameter
	PrefService prefService;
	
	/** List of parallelization profiles */
	private List<ParallelizationParadigmProfile> profiles;
	
	/** A string constant to be used by {@link PrefService} */
	private final String PROFILES = "profiles";
	
	// -- ParallelService methods --

	@Override
	public ParallelizationParadigm getParadigm() {
		List<ParallelizationParadigmProfile> selectedProfiles = getProfiles().stream()
				.filter(p -> p.isSelected().equals(true)).collect(Collectors.toList());

		if (selectedProfiles.size() == 1) {
			
			List<ParallelizationParadigm> foundParadigms = getInstances().stream()
					.filter(paradigm -> paradigm.getClass().equals(selectedProfiles.get(0).getParadigmType()))
					.collect(Collectors.toList());
			
			if (foundParadigms.size() == 1) {
				return foundParadigms.get(0);
			}
		}
		
		return null;
	}
	
	@Deprecated
	@SuppressWarnings("unchecked")
	@Override
	public <T extends ParallelizationParadigm> T getParadigm(
			final Class<T> desiredParalellizationParadigm) {
		List<ParallelizationParadigm> matchingParadigms = getInstances().stream()
				.filter(paradigm -> paradigm.getClass().equals(desiredParalellizationParadigm))
				.collect(Collectors.toList());
		
		if (matchingParadigms.size() == 1) {
			return (T) matchingParadigms.get(0);
		}
		
		return null;
	}
	
	@Override
	public List<ParallelizationParadigmProfile> getProfiles() {
		return profiles;
	}
	
	@Override
	public void addProfile(final ParallelizationParadigmProfile profile) {
		profiles.add(profile);
		saveProfiles();
	}
	
	@Override
	public void selectProfile(final String name) {
		profiles.forEach(p -> {
			if (p.getName().equals(name)) {
				p.setSelected(true);
			} else {
				p.setSelected(false);
			}
		});
		saveProfiles();
	}
	
	@Override
	public void deleteProfiles() {
		profiles.clear();
		saveProfiles();
	}
	
	// -- Service methods --

	@Override
	public void initialize() {
		retrieveProfiles();
	}

	// -- Helper methods --
	
	private void saveProfiles() {
		final List<String> serializedProfiles = new LinkedList<>();
		profiles.forEach(p -> {
			serializedProfiles.add(serializeProfile(p));
		});
		prefService.put(this.getClass(), PROFILES, serializedProfiles);
	}
	
	private void retrieveProfiles() {
		profiles = new LinkedList<>();
		prefService.getList(this.getClass(), PROFILES).forEach((serializedProfile) -> {
			profiles.add(deserializeProfile(serializedProfile));
		});
	}
	
	private String serializeProfile(final ParallelizationParadigmProfile profile) {
		try {
			final ByteArrayOutputStream baos = new ByteArrayOutputStream();
			final ObjectOutputStream oos = new ObjectOutputStream(baos);
	        oos.writeObject(profile);
	        oos.close();
	        return Base64.getEncoder().encodeToString(baos.toByteArray());
		} catch (Exception e) {
			// TODO: Proper error handling
		}
		return null;
	}
	
	private ParallelizationParadigmProfile deserializeProfile(final String serializedProfile) {
		try {
			final byte[] data = Base64.getDecoder().decode(serializedProfile);
			final ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
			final Object o = ois.readObject();
			ois.close();
			if (ParallelizationParadigmProfile.class.isAssignableFrom(o.getClass())) {
				return (ParallelizationParadigmProfile) o;
			}
		} catch (Exception e) {
			// TODO: Proper error handling
		}
		return null;
	}
	
}
