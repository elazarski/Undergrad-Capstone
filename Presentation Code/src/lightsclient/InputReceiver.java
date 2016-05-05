package lightsclient;

import java.util.concurrent.LinkedBlockingQueue;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;

import lightsclient.MyMessage.Type;

public class InputReceiver implements Receiver {

	private LinkedBlockingQueue<MyMessage> queue;
	private Part part;
	private boolean startPressed = false;
	private boolean jam = false;

	public static InputReceiver newInstance(Part p, LinkedBlockingQueue<MyMessage> playQueue) {
		InputReceiver ret = new InputReceiver();
		ret.setPart(p);
		ret.setQueue(playQueue);

		return ret;
	}

	private void setQueue(LinkedBlockingQueue<MyMessage> playQueue) {
		queue = playQueue;
	}

	private void setPart(Part p) {
		this.part = p;
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub
	}

	@Override
	public void send(MidiMessage message, long timeStamp) {
		// check for note on before sending to threadFunc
		// System.out.println(timeStamp + " " + System.nanoTime()/1000);
		if (startPressed && !jam) {
			if (message instanceof ShortMessage) {
				ShortMessage sm = (ShortMessage) message;
				int command = sm.getCommand();

				if (command == ShortMessage.NOTE_ON) {
					// System.out.println("GOT NOTE ON CHANNEL " +
					// part.getChannel());
					int noteNum = sm.getData1();
					if (part.isNext(noteNum)) {
						sendData(
								new MyMessage(part.getChannel(), Type.TIME_UPDATE, part.getTime(), part.getNextTime()));
					} else {
						Double time = part.isPossible(noteNum);

						if (time != null) {
							// sendData(new MyMessage(part.getChannel(),
							// Type.TIME_UPDATE, time, false));
							System.out.println(
									"working on new part, not sending message from channel: " + part.getChannel());
						}
					}
				}

				// check if done
				if (part.isDone()) {
					sendData(new MyMessage(part.getChannel(), Type.PART_DONE));
					System.out.println("SONG DONE: RECIEVER " + part.getChannel());
				}
			}
		}
	}

	private void sendData(MyMessage data) {
		queue.offer(data);
	}

	// notify with MyMessage
	public Double notify(MyMessage message) {
		// System.out.println(Thread.currentThread().getName());

		// parse message
		int channel = message.getChannel();
		// check type first
		switch (message.getType()) {
		case START:
			if (channel == 0) {
				startPressed = true;
				// System.out.println("START RECEIVED ON CHANNEL " +
				// part.getChannel());
			} else if (channel == 1) {
				jam = true;
			}
			break;
		case STOP:
			if (channel == 0) {
				// shouldn't have gotten here
				// remind PlayThread
				MyMessage snd = new MyMessage(part.getChannel(), Type.PART_DONE);
				sendData(snd);

				part.reset();
			} else if (channel == 1) {
				jam = false;
			}
			break;
		case TIME_UPDATE:
			// figure out what to do based upon channel
			Double newTime = null;
			switch (channel) {
			case 2: // next part
				System.out.println("Calling nextPart() from " + part.getChannel());
				newTime = part.nextPart();
				break;
			case 1: // next measure
				System.out.println("Calling nextMeasure() from " + part.getChannel());
				newTime = part.nextMeasure();
				break;
			case -2: // previous part
				newTime = part.previousPart();
				break;
			case -1: // previous measure
				newTime = part.previousMeasure();
				break;
			default:
				System.err.println("TIME_UPDATE WITH CHANNEL " + channel + " NOT IMPLEMENTED YET IN InputTReceiver");
				System.err.println(message.toString());
				break;
			}
			return newTime;
		default:
			System.err.println("MESSAGE NOT IMPLEMENTED YET IN InputReceiver");
			System.err.println(message.toString());
			break;
		}

		return null;
		// int channel = message.getChannel();
		// if (channel == 1) { // next part
		//
		// } else if (channel == 2) { // next measure
		//
		// } else if (channel == -1) { // previous part
		//
		// } else if (channel == -2) { // previous measure
		//
		// }
	}

	public void changeTime(double newTime) {
		// if part time or measure time != new time, change part
		if ((part.getPartTime() != newTime) || (part.getMeasureTime() != newTime)) {
			part.changeTime(newTime);
		}
	}

}
