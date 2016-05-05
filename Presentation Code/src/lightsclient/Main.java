package lightsclient;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.sound.midi.InvalidMidiDataException;

import lightsclient.MyMessage.Type;

public class Main {

	@SuppressWarnings("deprecation")
	public static void main(String[] args) {

		// create and start UI thread
		LinkedBlockingQueue<MyMessage> windowInQueue = new LinkedBlockingQueue<MyMessage>();
		LinkedBlockingQueue<MyMessage> windowOutQueue = new LinkedBlockingQueue<MyMessage>();
		Thread mainWindow = new Thread(new Runnable() {

			@Override
			public void run() {
				MainWindow.main(windowInQueue, windowOutQueue);
			}
		});
		mainWindow.setName("UI Thread");
		mainWindow.start();

		// create variables
		MyReader reader = new MyReader();
		Setlist setlist = new Setlist();
		MidiInterface m = new MidiInterface();

		// create playThread data, but do not start
		Thread playThread = null;
		LinkedBlockingQueue<MyMessage> playInQueue = new LinkedBlockingQueue<MyMessage>();
		LinkedBlockingQueue<MyMessage> playOutQueue = new LinkedBlockingQueue<MyMessage>();

		// main loop
		while (mainWindow.isAlive()) {
			try {
				MyMessage windowMessage = windowOutQueue.poll(10, TimeUnit.MILLISECONDS);
				MyMessage playMessage = playOutQueue.poll(10, TimeUnit.MILLISECONDS);
				MyMessage message;
				// String path;

				// check windowCommand
				if (windowMessage != null) {
					switch (windowMessage.getType()) {
					case SYSTEM_EXIT:
						// exit
						// if (playThread != null) {
						// playThread.stop();
						// }
						playInQueue.put(windowMessage);
						System.exit(0);

					case READ_FILE:
						// get path
						String filePath = (String) windowMessage.getData1();
						message = new MyMessage(null);
						// check if song or setlist
						if (windowMessage.getChannel() == 0) { // song
							Song s = reader.readSong(filePath);
							setlist.addSong(s);
							message.setData1(s.toString());
						} else { // setlist
							setlist = reader.readSetlist(filePath);
							message.setData1(setlist.getSongTitles());
						}
						// open song
						// get byte[] after command[1]
						// path = new String(Arrays.copyOfRange(windowCommand,
						// 1, windowCommand.length));
						// Song s = reader.readSong(path);
						// setlist.addSong(s);
						//
						// // update UI
						// windowQueue.put(s.toString().getBytes());

						// update UI
						windowInQueue.put(message);
						break;

					// case 0x2:
					// // open setlist
					// // get byte[] after command[1]
					// path = new String(Arrays.copyOfRange(windowCommand, 1,
					// windowCommand.length));
					// setlist = reader.readSetlist(path);
					//
					// // update UI
					// byte[] data = strToB(setlist.getSongTitles());
					// windowQueue.put(data);
					// break;
					//

					case MIDI_SELECTION:
						// select MIDI devices
						// send device names to window
						// byte[] inputNames = strToB(m.getInputNames());
						// windowQueue.put(inputNames);
						// byte[] outputNames = strToB(m.getOutputNames());
						// windowQueue.put(outputNames);

						// wait for device selection
						// byte[] selectionObj = windowQueue.take();
						// if (m != null) {
						// m.connect(selectionObj);
						// }

						// get names of MIDI devices
						String[] inputNames = m.getInputNames();
						String[] outputNames = m.getOutputNames();

						// send names to UI
						message = new MyMessage(Type.MIDI_SELECTION, inputNames, outputNames);
						windowInQueue.put(message);

						// wait for device selection
						message = windowOutQueue.take();
						m.connect((MidiSelection) message.getData1());
						break;

					case SETLIST_REORDER:
						// reorder setlist
						// get byte[] after command[1]
						// String[] newOrder = new
						// String(Arrays.copyOfRange(windowCommand, 1,
						// windowCommand.length)).split(Pattern.quote("|"));

						// get new order
						String[] newOrder = (String[]) windowMessage.getData1();
						setlist.reorder(newOrder);
						break;

					case START:
						// start button pressed
						// m.play(setlist, playQueue);
						final Setlist finalSetlist = setlist;
						playThread = new Thread(new Runnable() {

							@Override
							public void run() {
								m.play(finalSetlist, playInQueue, playOutQueue);
							}
						});
						playThread.setName("playThread");
						playThread.start();
						break;

					case STOP:
						// stop button pressed
						// System.out.println("stop");
						playInQueue.offer(windowMessage);
						break;

					default:
						// not implemented yet
						System.err.println("COMMAND FROM WINDOW NOT IMPLEMENTED YET");
						break;
					}
				}

				// check playCommand
				if (playMessage != null) {
					switch (playMessage.getType()) {
					case SONG_UPDATE:
						// // update UI with title of current song
						// byte[] title = Arrays.copyOfRange(playCommand, 1,
						// playCommand.length);
						// byte[] data = new byte[title.length + 1];
						// data[0] = 0x1;
						// for (int i = 0; i < title.length; i++) {
						// data[i + 1] = title[i];
						// }
						//
						// windowQueue.offer(data);

						windowInQueue.put(playMessage);
						break;

					default:
						System.err.println("COMMAND FROM PLAYTHREAD NOT IMPLEMENTED YET IN MAIN");
						break;

					}
				}
				// if (command[0].equals("song")) {
				// Song s = reader.readSong(command[1]);
				// setlist.addSong(s);
				// // update UI
				// String[] w = new String[1];
				// w[0] = s.toString();
				// windowQueue.put(w);
				//
				// } else if (command[0].equals("setlist")) {
				// setlist = reader.readSetlist(command[1]);
				//
				// // update UI
				// windowQueue.put(setlist.getSongTitles());
				//
				// } else if (command[0].equals("exit")) {
				// System.exit(0);
				// } else if (command[0].equals("reorder")) {
				// String[] newOrder = new String[command.length - 1];
				//
				// for (int i = 0; i < newOrder.length; i++) {
				// newOrder[i] = command[i + 1];
				// }
				//
				// setlist.reorder(newOrder);
				// } else if (command[0].equals("midi")) {
				// if (command[1].equals("names")) {
				// String[] inputNames = m.getInputNames();
				// windowQueue.put(inputNames);
				// String[] outputNames = m.getOutputNames();
				// windowQueue.put(outputNames);
				// }
				// }
			} catch (InterruptedException | IOException | InvalidMidiDataException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				if (mainWindow.isAlive()) {
					mainWindow.stop();
				}
				System.exit(1);
			}
		}

		// kill play thread if running
		if (playThread != null) {
			playThread.stop();
		}

	}

	// convert String[] to byte[]
	// private static byte[] strToB(String[] strings) {
	// String allStrings = new String();
	// for (int i = 0; i < strings.length; i++) {
	// allStrings = allStrings.concat(strings[i]);
	// allStrings = allStrings.concat("|");
	// }
	//
	// // remove last '|'
	// allStrings = allStrings.substring(0, allStrings.length() - 1);
	//
	// // convert allStrings to byte[]
	// byte[] ret = allStrings.getBytes();
	//
	// return ret;
	// }

}
