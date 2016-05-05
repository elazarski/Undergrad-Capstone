package lightsclient;

import java.util.concurrent.LinkedBlockingQueue;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;

import lightsclient.MyMessage.Type;

public class ControlReceiver implements Receiver {

	private LinkedBlockingQueue<MyMessage> out;

	public static ControlReceiver newInstance(LinkedBlockingQueue<MyMessage> q) {
		ControlReceiver ret = new ControlReceiver();
		ret.setQueue(q);

		return ret;

	}

	private void setQueue(LinkedBlockingQueue<MyMessage> q) {
		this.out = q;
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub

	}

	@Override
	public void send(MidiMessage message, long timeStamp) {
		// make sure of NOTE_ON event
		if (message instanceof ShortMessage) {
			ShortMessage sm = (ShortMessage) message;
			int command = sm.getCommand();

			if (command == ShortMessage.NOTE_ON) {
				System.out.println("Got control: " + sm.getData1());
				MyMessage snd = null;
				switch (sm.getData1()) {
				case 37: // START: C# (First black key)
					snd = new MyMessage(0, Type.START);
					break;
				case 39: // STOP: D# (Second black key)
					snd = new MyMessage(0, Type.STOP);
					break;
				case 41: // NEXT MEASURE (F)
					snd = new MyMessage(1, Type.TIME_UPDATE);
					break;
				case 43: // PREVIOUS MEASURE (G)
					snd = new MyMessage(-1, Type.TIME_UPDATE);
					break;
				case 45: // NEXP PART (A)
					snd = new MyMessage(2, Type.TIME_UPDATE);
					break;
				case 47: // PREVIOUS PART (B)
					snd = new MyMessage(-2, Type.TIME_UPDATE);
					break;
				case 61: // initiate jam session (Second C#)
					snd = new MyMessage(1, Type.START);
					break;
				case 63: // END JAM SESSION (Second D#)
					snd = new MyMessage(1, Type.STOP);
				default:
					System.err.println("Unrecognized input from command keyboard: " + sm.getData1());
				}

				// send message to main if it was instantiated
				if (snd != null) {
					out.offer(snd);
				}
			}

		}
	}

}
