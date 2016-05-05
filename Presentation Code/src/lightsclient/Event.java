package lightsclient;

public class Event {
	private int[] notes;
	private double time;

	private int numInChord = 0;
	private int possibleNumInChord = 0;

	public Event(String line) {
		String[] elements = line.split(" ");

		time = Double.parseDouble(elements[0]);

		// get notes
		notes = new int[elements.length - 1];
		for (int i = 1; i < elements.length; i++) {
			notes[i - 1] = Integer.parseInt(elements[i]);
		}
	}

	public Event(int[] notes, long time) {
		this.notes = notes;
		this.time = time;
	}

	public boolean isChord() {
		if (notes.length > 1) {
			return true;
		} else {
			return false;
		}
	}

	public boolean contains(int note) {

		for (int current : notes) {
			if (noteEquals(note, current)) {
				numInChord++;

				return true;
			}
		}

		return false;
	}

	public boolean possiblyContains(int note) {

		for (int current : notes) {
			if (noteEquals(note, current)) {
				possibleNumInChord++;

				return true;
			}
		}

		return false;
	}

	public void resetPossible() {
		possibleNumInChord = 0;
	}

	public void reset() {
		numInChord = 0;
		possibleNumInChord = 0;
	}

	private boolean noteEquals(int input, int current) {
		if ((input >= current - 1) && (input <= current + 1)) {
			return true;
		}

		return false;
	}

	public boolean isDone() {
		if (numInChord >= notes.length) {
			return true;
		} else {
			return false;
		}
	}

	public boolean possiblyIsDone() {
		if (possibleNumInChord >= notes.length) {
			return true;
		} else {
			return false;
		}
	}

	public double getTime() {
		return time;
	}

	public boolean equals(Event e) {
		e.resetPossible();

		for (int i = 0; i < notes.length; i++) {
			if (!e.possiblyContains(notes[i])) {
				e.resetPossible();
				return false;
			}
		}

		if (time != e.getTime()) {
			return false;
		}

		e.resetPossible();

		return true;
	}

	public int[] getAllNotes() {
		return notes;
	}

}
