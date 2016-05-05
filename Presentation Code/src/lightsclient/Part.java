package lightsclient;

import java.util.ArrayList;

public class Part {

	private int currentEvent = 0;
	// private int currentPhrase = 0;
	private int currentMeasure = 0;
	private int currentPart = 0;

	private int channel;
	private ArrayList<Event> notes;
	private ArrayList<Double> partTimes;
	private ArrayList<Double> measureTimes;

	private int possibleEvent = 0;
	// private int possiblePhrase = 0;

	private int[] partIndexes;
	private int[] measureIndexes;

	// private ArrayList<Integer> phrases = new ArrayList<Integer>();

	private ArrayList<Event> previousPossibleNotes = new ArrayList<Event>();

	public Part(int channel, String[] lines) {
		this.channel = channel;

		// initialize notes
		notes = new ArrayList<Event>(lines.length);
		for (String line : lines) {
			if (!line.equals("")) {
				notes.add(new Event(line));
			}
		}
	}

	public int getChannel() {
		return channel;
	}

	public double getTime() {
		return notes.get(currentEvent).getTime();
	}

	public double getNextTime() {
		if (currentEvent < notes.size()) {
			return notes.get(currentEvent + 1).getTime();
		} else {
			return 0;
		}
	}

	public double getPartTime() {
		return partTimes.get(currentPart);
	}

	public double getMeasureTime() {
		return measureTimes.get(currentMeasure);
	}

	public void addMeasures(ArrayList<Double> measures) {
		measureTimes = measures;

		// populate measureIndexes
		measureIndexes = new int[measureTimes.size()];
		double measureTime = measureTimes.get(currentMeasure);
		for (int i = 0; i < notes.size(); i++) {
			double eventTime = notes.get(i).getTime();

			// check if current time is past the measure marker
			if (eventTime >= measureTime) {
				// System.out.println(currentMeasure + " at index " + i);
				measureIndexes[currentMeasure] = i;
				currentMeasure++;

				// check next measures until this note's time is not past
				// the measure marker

				// get next measure if there is one
				if (currentMeasure == measureIndexes.length) {
					break;
				}
				measureTime = measureTimes.get(currentMeasure);
				while (eventTime >= measureTime) {
					// set measure index to current note
					measureIndexes[currentMeasure] = i;
					currentMeasure++;

					// make sure we have not gone further than the
					// number of measures
					if (currentMeasure == measureIndexes.length) {
						break;
					}

					// check next measure
					measureTime = measureTimes.get(currentMeasure);
				}
			}
			measureTime = measureTimes.get(currentMeasure);
		}

		// check whether or not we got through all measures
		if (!(currentMeasure >= measureIndexes.length)) {
			for (; currentMeasure < measureIndexes.length; currentMeasure++) {
				measureIndexes[currentMeasure] = notes.size();
			}
		}

		// reset currentMeasure
		currentMeasure = 0;
	}

	public void addParts(ArrayList<Double> parts) {
		partTimes = parts;

		// populate partIndexes
		partIndexes = new int[partTimes.size()];
		double partTime = partTimes.get(currentPart);
		for (int i = 0; i < notes.size(); i++) {
			double eventTime = notes.get(i).getTime();

			if (eventTime >= partTime) {
				partIndexes[currentPart] = i;
				currentPart++;

				if (currentPart > partIndexes.length) {
					break;
				}
				partTime = partTimes.get(currentMeasure);
				while (eventTime >= partTime) {
					partIndexes[currentPart] = i;
					currentPart++;

					if (currentPart > partIndexes.length) {
						break;
					}

					partTime = partTimes.get(currentPart);
				}
			}
			partTime = partTimes.get(currentPart);
		}

		if (!(currentPart >= partIndexes.length)) {
			for (; currentPart < partIndexes.length; currentPart++) {
				partIndexes[currentPart] = notes.size();
			}
		}

		currentPart = 0;
	}

	// method to determine phrases
	// check times and melodies to do this
	public void process() {
		for (int i = 0; i < measureIndexes.length; i++) {
			int beginIndex = measureIndexes[i];
			int endIndex = notes.size();

			if (i < measureIndexes.length - 1) {
				endIndex = measureIndexes[i + 1];
			}

			// get number of notes in current measure
			int measureSize = endIndex - beginIndex;
			if (measureSize > 0) {

				Event[] events = new Event[measureSize];
				for (int j = 0; j < events.length; j++) {
					events[j] = notes.get(beginIndex + j);
				}

				int[] lPhrases = Phrase.generate(events);
				for (int j = 0; j < lPhrases.length; j++) {
					lPhrases[j] += beginIndex;

					// lPhrases[j].offset(beginIndex);
					// phrases.add(lPhrases[j]);
				}
			}
		}
	}

	public void reset() {
		currentEvent = 0;
		// currentPhrase = 0;
		currentMeasure = 0;
		currentPart = 0;

		possibleEvent = 0;
		// possiblePhrase = 0;

		previousPossibleNotes = new ArrayList<Event>();

		for (Event ev : notes) {
			ev.reset();
		}
	}

	public boolean isNext(int input) {
		// System.out.println("Got note " + input);
		// if we have had 4 possible notes in a row, move to that note
		if (previousPossibleNotes.size() >= 4) {
			return false;
			// System.out.println("Moving from " + currentEvent + " to " +
			// possibleEvent);
			// currentEvent = possibleEvent;
			// previousCorrectNotes = new ArrayList<Event>();
		}

		// ask current event if this is the correct input
		Event ev = notes.get(currentEvent);
		boolean correct = ev.contains(input);

		// check for for correct note
		if (correct) {
			// check isDone to see if we should increment currentNote
			if (ev.isDone()) {
				currentEvent++;
				nextMP();

				// // check if recent notes are expected
				// // get events for previous perfect phrases
				// if (currentPhrase > 2) {
				// referencePhrase = currentPhrase - 2;
				// }
				//
				// Event[] lPhrases = new Event[(currentPhrase + 1) -
				// referencePhrase];
				// for (int i = 0; i < lPhrases.length; i++) {
				// lPhrases[i] = notes.get(referencePhrase + i);
				// }
				//
				// if (Phrase.similar(lPhrases, previousCorrectNotes.toArray(new
				// Event[1]))) {
				// System.out.println("Correct Phrase detected on channel " +
				// channel);
				// }
			}
			//
			// // set possibleEvent to currentEvent
			possibleEvent = currentEvent;
			//
			// // reset previous possible notes
			if (previousPossibleNotes.size() != 0) {
				previousPossibleNotes = new ArrayList<Event>();
			}
		}

		return correct;
	}

	public Double isPossible(int input) {
		// make sure that we aren't already looking at a possible new place
		if (possibleEvent != currentEvent) {
			Event ev = notes.get(possibleEvent);

			if (ev.possiblyContains(input)) {
				System.out.println("Looking at a new place, possibly " + possibleEvent);

				if (ev.possiblyIsDone()) {
					possibleEvent++;
					previousPossibleNotes.add(ev);

					return ev.getTime();
				}
			}
		}

		// check next 3 notes first, if any of these three, move to that
		// position
		for (int i = 1; i < 4; i++) {
			Event ev = notes.get(currentEvent + i);

			if (ev.possiblyContains(input)) {
				// look to the next note
				currentEvent = currentEvent + i + 1;
				possibleEvent = currentEvent;
				if (previousPossibleNotes.size() != 0) {
					previousPossibleNotes = new ArrayList<Event>();
				}

				return ev.getTime();
			}
		}

		// check next handful of notes
		Double time = possibleEvent(input, currentEvent);
		if (time != null) {
			System.out.println("Possible new note on channel " + channel);
			previousPossibleNotes.add(notes.get(possibleEvent));
			return time;
		}

		// check next few measures
		for (int i = currentMeasure + 1; i < currentMeasure + 4; i++) {
			if (i > measureIndexes.length) {
				break;
			}
			int index = measureIndexes[i];
			time = possibleEvent(input, index);

			if (time != null) {
				System.out.println("Possible new measure on channel " + channel);
				previousPossibleNotes.add(notes.get(possibleEvent));
				return time;
			}
		}

		// check next couple of parts
		for (int i = currentPart + 1; i < currentPart + 3; i++) {
			if (i > partIndexes.length) {
				break;
			}
			int index = partIndexes[i];
			time = possibleEvent(input, index);

			if (time != null) {
				System.out.println("Possible new part on channel " + channel);
				previousPossibleNotes.add(notes.get(possibleEvent));
				return time;
			}
		}

		// no possible place found
		return null;
	}

	private Double possibleEvent(int input, int index) {
		// check next handful of events
		for (int i = 1; i < 7; i++) {
			if ((index + i) > notes.size()) {
				break;
			}

			Event ev = notes.get(index + i);

			if (ev.possiblyContains(input)) {

				if (ev.possiblyIsDone()) {
					// look to next possibleEvent
					possibleEvent = index + i + 1;
					// previousPossibleNotes.add(input);

					return ev.getTime();
				}
			}
		}

		return null;
	}

	public boolean isDone() {
		if (currentEvent == notes.size()) {
			System.out.println("SONG DONE: " + channel);
			return true;
		} else {
			return false;
		}
	}

	// check if we have changed measures or parts
	private void nextMP() {
		// check phrases first
		// int nextPhrase = phrases.get(currentPhrase + 1);
		// if (currentEvent >= nextPhrase) {
		// currentPhrase++;
		// }

		// check measure next
		// get time
		int nextMeasure = measureIndexes[currentMeasure + 1];
		while (currentEvent >= nextMeasure) {
			currentMeasure++;

			nextMeasure = measureIndexes[currentMeasure + 1];
		}
		// if (currentEvent >= nextMeasure) {
		// currentMeasure++;
		// }

		// check parts last
		// get time
		int nextPart = partIndexes[currentMeasure + 1];
		while (currentEvent >= nextPart) {
			currentPart++;

			nextPart = partIndexes[currentPart];
		}
		// if (currentEvent >= nextPart) {
		// currentPart++;
		// }
	}

	private void findForwardMP() {
		// phrases first
		// int phraseIndex = phrases.get(currentPhrase);
		// while (currentEvent > phraseIndex) {
		// currentPhrase++;
		//
		// phraseIndex = phrases.get(currentPhrase);
		// }

		// measures next
		int measureIndex = measureIndexes[currentMeasure];
		while (currentEvent >= measureIndex) {
			currentMeasure++;

			measureIndex = measureIndexes[currentMeasure];
		}

		// parts last
		int partIndex = partIndexes[currentPart];
		while (currentEvent >= partIndex) {
			currentPart++;

			partIndex = partIndexes[currentPart];
		}
	}

	private void findBackwardMP() {
		// phrases first
		// int phraseIndex = phrases.get(currentPhrase);
		// while (currentEvent < phraseIndex) {
		// currentPhrase--;
		//
		// phraseIndex = phrases.get(currentPhrase);
		// }

		// measures next
		int measureIndex = measureIndexes[currentMeasure];
		while (currentEvent < measureIndex) {
			currentMeasure--;

			measureIndex = measureIndexes[currentMeasure];
		}

		// parts last
		int partIndex = partIndexes[currentPart];
		while (currentEvent < partIndex) {
			currentPart--;

			partIndex = partIndexes[currentPart];
		}
	}

	public Double nextPart() {
		// make sure we are not out of parts
		if (currentPart == partTimes.size()) {
			return null;
		}

		System.out.print(channel + ": Part " + currentPart + " -> ");
		currentPart++;
		System.out.print(currentPart + ", Measure " + currentMeasure + " -> ");

		// check to see if we are waiting for the start of a new part
		// if new currentPart == next note
		// if (partTimes.get(currentPart) == notes.get(currentEvent +
		// 1).getTime()) {
		// currentPart++;
		// }
		if (partIndexes[currentPart] == currentEvent + 1) {
			currentPart++;
		}

		// find new currentNote and currentMeasure
		double partTime = partTimes.get(currentPart);

		// measures first
		while (measureTimes.get(currentMeasure) < partTime) {
			currentMeasure++;
		}

		System.out.print(currentMeasure + ", Event " + currentEvent + " -> ");
		// note
		while (notes.get(currentEvent).getTime() < partTime) {
			currentEvent++;
		}

		System.out.println(currentEvent);

		return partTimes.get(currentPart);
	}

	public Double nextMeasure() {
		// make sure we are not out of measures
		if (currentMeasure == measureTimes.size()) {
			return null;
		}

		System.out.print(channel + ": Measure " + currentMeasure + " -> ");
		currentMeasure++;
		System.out.print(currentMeasure + ", Part " + currentPart + " -> ");

		// make sure we are not waiting for the current measure to start
		// if the new currentMeasure == next note
		if (measureTimes.get(currentMeasure) == notes.get(currentEvent + 1).getTime()) {
			currentMeasure++;
		}

		// find new currentNote and currentPart
		double measureTime = measureTimes.get(currentMeasure);

		// part first
		// check if we even have to move
		double nextPart = partTimes.get(currentPart + 1);
		if (nextPart <= measureTime) {
			while (partTimes.get(currentPart) < measureTime) {
				currentPart++;
			}
		}
		System.out.print(currentPart + ", Event " + currentEvent + " -> ");

		// new note
		while (notes.get(currentEvent).getTime() < measureTime) {
			currentEvent++;
		}

		System.out.println(currentEvent);

		return measureTimes.get(currentMeasure);
	}

	public Double previousPart() {
		// make sure we are not at the beginning of the song still
		if (currentPart == 0) {
			currentMeasure = 0;
			currentEvent = 0;
			return new Double(0);
		}

		currentPart--;

		// find new currentNote and currentMeasure
		double partTime = partTimes.get(currentPart);

		// measures first
		while (measureTimes.get(currentMeasure - 1) > partTime) {
			currentMeasure--;
		}

		// new note
		while (notes.get(currentEvent - 1).getTime() > partTime) {
			currentEvent--;
		}

		return partTimes.get(currentPart);
	}

	public Double previousMeasure() {
		// make sure we are not at the beginning of the song still
		if (currentMeasure == 0) {
			currentPart = 0;
			currentEvent = 0;
			return new Double(0);
		}

		currentMeasure--;

		// find new currentNote and currentPart
		double measureTime = measureTimes.get(currentMeasure);

		// check if even necessary for part
		if (partTimes.get(currentPart - 1) > measureTime) {
			while (partTimes.get(currentPart) > measureTime) {
				currentPart--;
			}
		}

		// notes
		// make sure next note is not less than (before) measureTime
		while (notes.get(currentEvent - 1).getTime() > measureTime) {
			currentEvent--;
		}

		return measureTimes.get(currentMeasure);
	}

	public void changeTime(double newTime) {
		if (newTime > getTime()) {
			// if we are behind, find new note
			while (getTime() < newTime) {
				currentEvent++;
			}

			// move MP accordingly
			findForwardMP();

		} else {
			// if we are ahead, find new note
			while (getTime() > newTime) {
				currentEvent--;
			}

			// move MP accordingly
			findBackwardMP();
		}
	}
}

//// checks if input is acceptable to increase current note
// public boolean isNext(int input) {
//
// // check if we are in a chord
// if (inChord) {
//
// // create array to loop through
// int chordLen = chords.get(currentChord).get(1) -
//// chords.get(currentChord).get(0);
// for (int i = 0; i < chordLen; i++) {
// ShortMessage sm = (ShortMessage) track.get(currentNote + i).getMessage();
// int correctNote = sm.getData1();
//
// if ((input >= correctNote - 2) && (input <= correctNote + 2)) {
// currentNote++;
// numInChord++;
//
// // check if chord is done
// if (numInChord >= chordLen) {
// inChord = false;
// numInChord = 0;
//
// //checkNextMP();
// }
//
// return true;
// }
// }
// } else {
//
// ShortMessage sm = (ShortMessage)track.get(currentNote).getMessage();
// int correctNote = sm.getData1();
//
// if ((input >= correctNote - 2) && (input <= correctNote + 2)) {
// currentNote++;
// //checkNextMP();
//
// return true;
// }
// }
//
// return false;
// }
//
// public boolean done() {
// if (currentNote == track.size()) {
// return true;
// } else {
// return false;
// }
// }
//
// private void checkNextMP() {
// // check if starting next measure or part
// long nextTick = track.get(currentNote).getTick();
// if (nextTick == measureTimes.get(currentMeasure + 1)) {
// currentMeasure++;
// if (nextTick == partTimes.get(currentPart + 1)) {
// currentPart++;
// }
// }
// }

// public Part(int channel, Song s) {
// currentNote = 0;
// currentChord = 0;
// inChord = false;
// numInChord = 0;
// currentMeasure = 0;
// currentPart = 0;
//
// outputTracks = new ArrayList<Track>();
// chords = new ArrayList<ArrayList<Integer> >();
//
// this.channel = channel;
// Sequence seq = s.getInputTrack(channel);
// ArrayList<Sequence> outputSeq = s.getOutputTracks();
//
// Track[] t = seq.getTracks();
// track = t[0];
//
//// make sure all events are 0x90
// for (int i = 0; i < track.size();) {
// MidiEvent ev = track.get(i);
// MidiMessage m = ev.getMessage();
//
// if (m instanceof ShortMessage) {
// ShortMessage sm = (ShortMessage)m;
//
// if (sm.getCommand() != ShortMessage.NOTE_ON) {
// // System.out.println("Not Note_on: " + sm.getCommand());
// track.remove(ev);
// } else {
// // System.out.println("NOTE_ON");
// i++;
// }
// } else {
// // System.out.println("Not ShortMessage");
// i++;
// }
// }
//
//// add other tracks if needed
// for (int i = 1; i < t.length; i++) {
// //System.out.println(t[i].size());
// for (int j = 0; j < t[i].size(); j++) {
//
// // make sure event is 0x90
// MidiEvent ev = t[i].get(i);
// MidiMessage m = ev.getMessage();
//
// if (m instanceof ShortMessage) {
// ShortMessage sm = (ShortMessage)m;
// // System.out.println(sm.getCommand());
// if (sm.getCommand() == 0x90) {
// System.out.println("NOTE_ON");
// track.add(ev);
// }
// }
// }
// }
//
// System.out.println("Track length: " + track.size());
//
//// populate chords
// long previousTime = 0;
// int numChords = 0;
// chords.add(new ArrayList<Integer>(2));
// boolean inChord = false;
// for (int i = 1; i < track.size(); i++) {
// // get current time
// long currentTime = track.get(i).getTick();
// System.out.println(track.get(i).getTick());
//
// // check if the past two notes have the same time
// if (currentTime == previousTime) {
// // the past two notes are played at the same time (a chord)
//
// // check if we are already in a chord by checking chords
// if (inChord) {
// // previous chord is unfinished
//
// // check if this is the last note in a chord
// long nextTime = track.get(i + 1).getTick();
// if (currentTime != nextTime) {
// // chord is ending
// chords.get(numChords).add(i);
// inChord = false;
// System.out.println("Chord ends at " + i);
// }
// } else {
// // we must be at the second note in a chord
// // start new chord in chords
// chords.add(new ArrayList<Integer>(2));
// numChords++;
// inChord = true;
// chords.get(numChords).add(i - 1);
// System.out.println("Chord begins at " + (i - 1));
// }
//
// }
// }
//
//// turn sequences in outputSeq into tracks
// for (int i = 0; i < outputSeq.size(); i++) {
// t = outputSeq.get(i).getTracks();
// outputTracks.add(t[0]);
// for (int j = 1; j < t.length; j++) {
// for (int h = 0; h < t[j].size(); h++) {
// outputTracks.get(i).add(t[j].get(h));
// }
// }
// }
//
// this.partTimes = s.getPartTimes();
// this.measureTimes = s.getMeasureTimes();
// }
// =======
// package lightsclient;
//
// import java.util.ArrayList;
//
// public class Part {
//
// private int currentEvent;
// private int currentMeasure;
// private int currentPart;
//
// private int channel;
// private ArrayList<Event> notes;
// private ArrayList<Long[]> outputTimes;
// private ArrayList<Long> partTimes;
// private ArrayList<Long> measureTimes;
//
//
//
// public Part(int channel, String[] lines) {
// this.channel = channel;
// partTimes = new ArrayList<Long>();
// measureTimes = new ArrayList<Long>();
// outputTimes = new ArrayList<Long[]>();
//
// // initialize notes
// notes = new ArrayList<Event>(lines.length);
//
// for (String line : lines) {
// if (!line.equals("")) {
// notes.add(new Event(line));
// }
// }
//
// // initialize CURRENT variables
// currentEvent = 0;
// currentMeasure = 0;
// currentPart = 0;
// }
//
// public int getChannel() {
// return channel;
// }
//
// public void addOutputTimes(long[] p) {
// Long[] temp = new Long[p.length];
// for (int i = 0; i < p.length; i++) {
// temp[i] = p[i];
// }
//
// outputTimes.add(temp);
// }
//
// public void addMeasures(ArrayList<Long> measures) {
// this.measureTimes = measures;
// }
//
// public void addParts(ArrayList<Long> parts) {
// this.partTimes = parts;
// }
//
// public boolean isNext(int input) {
// // ask current event if this is the correct input
// Event ev = notes.get(currentEvent);
//
// boolean correct = ev.contains(input);
//
// // check for next MP if correct note
// if (correct) {
// nextMP(ev);
//
// // check isDone to see if we should increment currentNote
// if (ev.isDone()) {
// currentEvent++;
// }
// }
//
// return correct;
// }
//
// public boolean isDone() {
// if (currentEvent == notes.size()) {
// System.out.println("SONG DONE: " + channel);
// return true;
// } else {
// return false;
// }
// }
//
// // check if we have changed measures or parts
// private void nextMP(Event ev) {
// // check measure first
// // get time
// long nextMeasure = measureTimes.get(currentMeasure + 1);
// if (ev.getTime() >= nextMeasure) {
// currentMeasure++;
// }
//
// // check parts next
// // get time
// long nextPart = partTimes.get(currentPart + 1);
// if (ev.getTime() >= nextPart) {
// currentPart++;
// }
// }
//
// public void nextPart() {
// currentPart++;
// long partTime = partTimes.get(currentPart);
//
// // get to next measure
// boolean found = false;
// int newMeasure = currentMeasure;
// System.out.println("OLD MEASURE: " + currentMeasure);
// while (!found) {
// long time = measureTimes.get(newMeasure);
// if (time >= partTime) {
// currentMeasure = newMeasure;
// found = true;
// System.out.println("NEW MEASURE: " + currentMeasure);
// } else {
// newMeasure++;
// }
// }
//
// // find new event
// found = false;
// int newEvent = currentEvent;
// System.out.println("OLD NOTE: " + currentEvent);
// while (!found) {
// long time = notes.get(newEvent).getTime();
// if (time >= partTime) {
// currentEvent = newEvent;
// found = true;
// System.out.println("NEW NOTE: " + currentEvent);
// } else {
// newEvent++;
// }
// }
// }
//
// public void nextMeasure() {
// currentMeasure++;
// long measureTime = measureTimes.get(currentMeasure);
//
// // check for part update
// if (measureTime >= partTimes.get(currentPart)) {
// currentPart++;
// }
//
// // find next event
// boolean found = false;
// int newEvent = currentEvent;
// while (!found) {
// long time = notes.get(newEvent).getTime();
// if (time >= measureTime) {
// currentEvent = newEvent;
// found = true;
// } else {
// newEvent++;
// }
// }
// }
// }

//// checks if input is acceptable to increase current note
// public boolean isNext(int input) {
//
// // check if we are in a chord
// if (inChord) {
//
// // create array to loop through
// int chordLen = chords.get(currentChord).get(1) -
//// chords.get(currentChord).get(0);
// for (int i = 0; i < chordLen; i++) {
// ShortMessage sm = (ShortMessage) track.get(currentNote + i).getMessage();
// int correctNote = sm.getData1();
//
// if ((input >= correctNote - 2) && (input <= correctNote + 2)) {
// currentNote++;
// numInChord++;
//
// // check if chord is done
// if (numInChord >= chordLen) {
// inChord = false;
// numInChord = 0;
//
// //checkNextMP();
// }
//
// return true;
// }
// }
// } else {
//
// ShortMessage sm = (ShortMessage)track.get(currentNote).getMessage();
// int correctNote = sm.getData1();
//
// if ((input >= correctNote - 2) && (input <= correctNote + 2)) {
// currentNote++;
// //checkNextMP();
//
// return true;
// }
// }
//
// return false;
// }
//
// public boolean done() {
// if (currentNote == track.size()) {
// return true;
// } else {
// return false;
// }
// }
//
// private void checkNextMP() {
// // check if starting next measure or part
// long nextTick = track.get(currentNote).getTick();
// if (nextTick == measureTimes.get(currentMeasure + 1)) {
// currentMeasure++;
// if (nextTick == partTimes.get(currentPart + 1)) {
// currentPart++;
// }
// }
// }

// public Part(int channel, Song s) {
// currentNote = 0;
// currentChord = 0;
// inChord = false;
// numInChord = 0;
// currentMeasure = 0;
// currentPart = 0;
//
// outputTracks = new ArrayList<Track>();
// chords = new ArrayList<ArrayList<Integer> >();
//
// this.channel = channel;
// Sequence seq = s.getInputTrack(channel);
// ArrayList<Sequence> outputSeq = s.getOutputTracks();
//
// Track[] t = seq.getTracks();
// track = t[0];
//
//// make sure all events are 0x90
// for (int i = 0; i < track.size();) {
// MidiEvent ev = track.get(i);
// MidiMessage m = ev.getMessage();
//
// if (m instanceof ShortMessage) {
// ShortMessage sm = (ShortMessage)m;
//
// if (sm.getCommand() != ShortMessage.NOTE_ON) {
// // System.out.println("Not Note_on: " + sm.getCommand());
// track.remove(ev);
// } else {
// // System.out.println("NOTE_ON");
// i++;
// }
// } else {
// // System.out.println("Not ShortMessage");
// i++;
// }
// }
//
//// add other tracks if needed
// for (int i = 1; i < t.length; i++) {
// //System.out.println(t[i].size());
// for (int j = 0; j < t[i].size(); j++) {
//
// // make sure event is 0x90
// MidiEvent ev = t[i].get(i);
// MidiMessage m = ev.getMessage();
//
// if (m instanceof ShortMessage) {
// ShortMessage sm = (ShortMessage)m;
// // System.out.println(sm.getCommand());
// if (sm.getCommand() == 0x90) {
// System.out.println("NOTE_ON");
// track.add(ev);
// }
// }
// }
// }
//
// System.out.println("Track length: " + track.size());
//
//// populate chords
// long previousTime = 0;
// int numChords = 0;
// chords.add(new ArrayList<Integer>(2));
// boolean inChord = false;
// for (int i = 1; i < track.size(); i++) {
// // get current time
// long currentTime = track.get(i).getTick();
// System.out.println(track.get(i).getTick());
//
// // check if the past two notes have the same time
// if (currentTime == previousTime) {
// // the past two notes are played at the same time (a chord)
//
// // check if we are already in a chord by checking chords
// if (inChord) {
// // previous chord is unfinished
//
// // check if this is the last note in a chord
// long nextTime = track.get(i + 1).getTick();
// if (currentTime != nextTime) {
// // chord is ending
// chords.get(numChords).add(i);
// inChord = false;
// System.out.println("Chord ends at " + i);
// }
// } else {
// // we must be at the second note in a chord
// // start new chord in chords
// chords.add(new ArrayList<Integer>(2));
// numChords++;
// inChord = true;
// chords.get(numChords).add(i - 1);
// System.out.println("Chord begins at " + (i - 1));
// }
//
// }
// }
//
//// turn sequences in outputSeq into tracks
// for (int i = 0; i < outputSeq.size(); i++) {
// t = outputSeq.get(i).getTracks();
// outputTracks.add(t[0]);
// for (int j = 1; j < t.length; j++) {
// for (int h = 0; h < t[j].size(); h++) {
// outputTracks.get(i).add(t[j].get(h));
// }
// }
// }
//
// this.partTimes = s.getPartTimes();
// this.measureTimes = s.getMeasureTimes();
// }

// public void addParts(ArrayList<Long> parts) {
// ArrayList<Integer> tempPartIndexes = new ArrayList<Integer>(parts.size());
// for (int i = 0; i < parts.size(); i++) {
// tempPartIndexes.add(0);
// }
//
// int part = 0;
// for (int i = 0; i < notes.size(); i++) {
// Event ev = notes.get(i);
// long time = ev.getTime();
//
// if (time >= parts.get(part)) {
// tempPartIndexes.set(part, i);
// part++;
//
// i++;
// ev = notes.get(i);
// time = ev.getTime();
// while (time > parts.get(part)) {
// tempPartIndexes.set(part - 1, -1);
// parts.remove(part - 1);
// part++;
// i++;
// ev = notes.get(i);
// time = ev.getTime();
// }
//
// part++;
//
// if (part >= parts.size()) {
// break;
// }
// }
//
// this.partTimes = parts;
// this.partIndexes = tempPartIndexes.toArray(new
// Integer[tempPartIndexes.size()]);
// }

// this.partTimes = parts;
//
//// populate partIndexes
// this.partIndexes = new int[partTimes.size()];
//
//// go through notes and part times
// int part = 0;
// for (int i = 0; i < notes.size(); i++) {
// Event ev = notes.get(i);
//
// // if current event time >= current part time, then add value to partIndexes
// // if >, then we have passed the time for the next part, the current on will
// suffice
// if (ev.getTime() >= partTimes.get(part)) {
// partIndexes[part] = i;
// part++;
// }
// }
// }

// public void addMeasures(ArrayList<Long> measures) {
// ArrayList<Integer> tempMeasureIndexes = new
// ArrayList<Integer>(measures.size());
// for (int i = 0; i < measures.size(); i++) {
// tempMeasureIndexes.add(-1);
// }
//
// int measure = 0;
// for (int i = 0; i < notes.size(); i++) {
// Event ev = notes.get(i);
// long time = ev.getTime();
//
// // if the current note has passed a measure index
// if (time >= measures.get(measure)) {
//
// // add current index to array
// tempMeasureIndexes.set(measure, i);
// //measure++;
//
// // if next note is also past the current measure,
// // get rid of the current measure
// // repeat until next note is not after the
// // measure being examined
// i++;
// ev = notes.get(i);
// time = ev.getTime();
// while (time > measures.get(measure)) {
// tempMeasureIndexes.set(measure, -1);
// measures.remove(measure);
// measure++;
// i++;
// ev = notes.get(i);
// time = ev.getTime();
// }
//
// measure++;
// if (measure >= measures.size()) {
// break;
// }
// }
// }
//
//// remove all -1's from tempMeasureIndexes
// int i = 0;
// while (i < tempMeasureIndexes.size()) {
// if (tempMeasureIndexes.get(i) == -1) {
// tempMeasureIndexes.remove(i);
// } else {
// i++;
// }
// }
//
// this.measureTimes = measures;
// this.measureIndexes = tempMeasureIndexes.toArray(new
// Integer[tempMeasureIndexes.size()]);
//// this.measureTimes = measures;
////
////// populate measureIndexes
//// this.measureIndexes = new int[measureTimes.size()];
////
////// go through notes and measure times
//// int measure = 0;
//// for (int i = 0; i < notes.size(); i++) {
//// Event ev = notes.get(i);
//// long time = ev.getTime();
////
//// // if current event time >= current measure time, then add value to
// measureIndexes
//// // if >, then we have passed the time for the next measure, so the current
// index will suffice
//// if (time >= measureTimes.get(measure)) {
//// measureIndexes[measure] = i;
//// measure++;
////
//// // make sure that we did not just add an empty measure
//// while (time > measureTimes.get(measure)) {
////
//// // remove these later
//// measureIndexes[measure-1] = -1;
//// measure++;
//// }
//// }
//// }
////
////// remove empty measures
//// ArrayList<Long> oldMeasureTimes = measureTimes;
//// ArrayList<Long> newMeasureTimes = new
// ArrayList<Long>(oldMeasureTimes.size());
////
//// for (int i = 0; i < oldMeasureTimes.size(); i++) {
//// int index = measureIndexes[i];
//// }
// }