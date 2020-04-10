package org.cimug.compare;

public enum Status {
	Identical("Identical"), BaselineOnly("Baseline only"), ModelOnly("Model only"), Changed("Changed"), Moved("Moved");

	private final String description;

	Status(String description) {
		this.description = description;
	}

	@Override
	public String toString() {
		return description;
	}

}