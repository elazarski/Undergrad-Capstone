package lightsclient;

import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;

public class OutputPart {

	private int channel;
	private Receiver receiver;

	private int currentNote = 0;
	private ArrayList<Event> notes;
	private boolean done = false;

	private double songTime;

	public OutputPart(String[] lines, int channel) {
		notes = new ArrayList<Event>(lines.length);

		for (String line : lines) {
			if (!line.equals("")) {
				notes.add(new Event(line));
			}
		}

		this.channel = channel;
	}

	public double[] getTimes() {
		double[] ret = new double[notes.size()];

		for (int i = 0; i < notes.size(); i++) {
			ret[i] = notes.get(i).getTime();
		}

		return ret;
	}

	/**
	 * @return the output
	 */
	public Receiver getOutput() {
		return receiver;
	}

	/**
	 * @param output
	 *            the output to set
	 */
	public void setOutput(Receiver output) {
		this.receiver = output;

		// output notes at time 0
		// if (notes.get(currentNote).getTime() == 0) {
		// Event currentEvent = notes.get(currentNote);
		// // construct MIDI message
		// // MidiMessage message = new ShortMessage(ShortMessage.NOTE_ON, 0, ,
		// // 97);
		// for (int note : currentEvent.getAllNotes()) {
		// try {
		// MidiMessage message = new ShortMessage(ShortMessage.NOTE_ON, 0, note,
		// 97);
		// output.send(message, System.nanoTime());
		// } catch (InvalidMidiDataException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		// }
		// currentNote++;
		// }
	}

	public void outputFirstEvents() {
		if (notes.get(currentNote).getTime() == 0) {
			Event currentEvent = notes.get(currentNote);
			for (int note : currentEvent.getAllNotes()) {
				try {
					MidiMessage message = new ShortMessage(ShortMessage.NOTE_ON, 0, note, 97);
					receiver.send(message, System.nanoTime());
				} catch (InvalidMidiDataException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			currentNote++;
		}
	}

	private boolean ready() {
		double currentEventTime = notes.get(currentNote).getTime();
		if (songTime >= currentEventTime) {
			return true;
		}
		return false;
	}

	public void reset() {
		currentNote = 0;
		done = false;
	}

	public int getChannel() {
		return channel;
	}

	public void play(LinkedBlockingQueue<MyMessage> input) {
		// this.input = input;

		// System.out.println(Thread.currentThread().getName());

		while (!done) {
			MyMessage message = null;
			try {
				message = input.poll(1, TimeUnit.MICROSECONDS);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			// check timing
			// long time = System.currentTimeMillis() % 1000;

			if (message != null) {
				// System.out.println("GOT MESSAGE");
				// check message type
				switch (message.getType()) {
				case TIME_UPDATE:
					songTime = (double) message.getData1();

					sendIfReady();
					break;
				default:
					System.err.println(message.toString() + " not implemented yet in output part!");
					break;
				}
				// System.out.println("GOT MESSAGE");
				// double partTime = (Double) message.getData1();
				// double nextTime = (Double) message.getData1();
				// int partChannel = message.getChannel();
				//
				// currentInputTimes[partChannel] = partTime;
				// nextInputTimes[partChannel] = nextTime;
				//
				// // check if we should listen to this part for now
				// double timeDiff = nextTime - partTime;
				// if (timeDiff > 0 && timeDiff > 5) {
				// // 5 for temporary fixing with changing types
				// listen[partChannel] = false;
				// }

			}

		}
	}

	private void sendIfReady() {
		// send event if ready
		while (ready()) {
			Event currentEvent = notes.get(currentNote);
			for (int note : currentEvent.getAllNotes()) {
				try {
					MidiMessage message = new ShortMessage(ShortMessage.NOTE_ON, 0, note, 97);
					receiver.send(message, System.nanoTime());
				} catch (InvalidMidiDataException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			currentNote++;
		}

		if (currentNote >= notes.size()) {
			done = true;
		}
	}

	public void notify(MyMessage message) {

	}
}
