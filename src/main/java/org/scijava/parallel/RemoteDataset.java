package org.scijava.parallel;

import java.net.URI;

public abstract class RemoteDataset {

	private URI uri;

	public RemoteDataset(URI uri) {
		super();
		this.uri = uri;
	}
	
	
	public URI getUri() {
		return uri;
	}
	

}
