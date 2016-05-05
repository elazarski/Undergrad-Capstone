package lightsclient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Transmitter;

import lightsclient.MyMessage.Type;

public class MidiInterface {

	private MidiDevice.Info[] infos;

	private ArrayList<MidiDevice> inputDevices;
	private ArrayList<Transmitter> inputTransmitters;
	private ArrayList<InputReceiver> inputReceivers;

	private Transmitter controlTransmitter;
	private MidiDevice controlDevice;
	private ControlReceiver controlReceiver;

	private ArrayList<MidiDevice> outputDevices;
	private ArrayList<Receiver> outputReceivers;

	private LinkedBlockingQueue<MyMessage> inQueue;
	private LinkedBlockingQueue<MyMessage> outQueue;

	public MidiInterface() {
		infos = MidiSystem.getMidiDeviceInfo();

		inputDevices = new ArrayList<MidiDevice>();
		inputTransmitters = new ArrayList<Transmitter>();
		inputReceivers = new ArrayList<InputReceiver>();
		outputDevices = new ArrayList<MidiDevice>();
		outputReceivers = new ArrayList<Receiver>();
	}

	public String[] getInputNames() {
		ArrayList<String> listRet = new ArrayList<String>();

		// code found at:
		// http://stackoverflow.com/questions/6937760/java-getting-input-from-midi-keyboard
		for (int i = 0; i < infos.length; i++) {
			try {
				MidiDevice device = MidiSystem.getMidiDevice(infos[i]);

				// get name if it is an output port (available input)
				if (device.getMaxTransmitters() != 0) {
					listRet.add(device.getDeviceInfo().getName().toString());
				}
			} catch (MidiUnavailableException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		String[] ret = new String[listRet.size()];
		ret = listRet.toArray(ret);
		return ret;
	}

	public String[] getOutputNames() {
		ArrayList<String> listRet = new ArrayList<String>();

		// code found at:
		// http://stackoverflow.com/questions/6937760/java-getting-input-from-midi-keyboard
		for (int i = 0; i < infos.length; i++) {
			try {
				MidiDevice device = MidiSystem.getMidiDevice(infos[i]);

				// get name if this is an input port (available output)
				if (device.getMaxReceivers() != 0) {
					listRet.add(device.getDeviceInfo().getName().toString());
				}
			} catch (MidiUnavailableException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		String[] ret = new String[listRet.size()];
		ret = listRet.toArray(ret);
		return ret;
	}

	public void connect(MidiSelection selection) {
		try {
			// MidiSelection selection =
			// MidiSelection.deserialize(selectionObj);

			// set sizes for MIDI I/O ArrayLists
			int maxInputChannel = selection.getMaxInputChannel();
			inputDevices = new ArrayList<MidiDevice>(maxInputChannel);
			inputTransmitters = new ArrayList<Transmitter>(maxInputChannel);

			int maxOutputChannel = selection.getMaxOutputChannel();
			outputDevices = new ArrayList<MidiDevice>(maxOutputChannel);
			outputReceivers = new ArrayList<Receiver>(maxOutputChannel);

			for (int i = 0; i < maxInputChannel; i++) {
				inputDevices.add(null);
				inputTransmitters.add(null);
				inputReceivers.add(null);
			}

			for (int i = 0; i < maxOutputChannel; i++) {
				outputDevices.add(null);
				outputReceivers.add(null);
			}

			controlDevice = null;

			// attempt to connect based upon name
			for (int i = 0; i < infos.length; i++) {
				MidiDevice device = MidiSystem.getMidiDevice(infos[i]);

				// get name of device
				String name = device.getDeviceInfo().getName().toString();

				// check for output port (available input)
				if (device.getMaxTransmitters() != 0) {

					// get channel based upon name
					int channel = selection.getInputChannel(name);

					// connect if channel is >= 0
					if (channel > 0) {
						inputTransmitters.set(channel - 1, device.getTransmitter());
						inputDevices.set(channel - 1, device);

					} else if (channel == 0) { // control input
						controlTransmitter = device.getTransmitter();
						controlDevice = device;
					}
				} else if (device.getMaxReceivers() != 0) { // input port
															// (available
															// output)
					// get channel based upon name
					int channel = selection.getOutputChannel(name);

					// connect if channel is >= 1
					if (channel >= 0) {
						outputReceivers.set(channel - 1, device.getReceiver());
						outputDevices.set(channel - 1, device);
					}
				}
			}

			// connect to input and output devices
			// for (MidiDevice current : inputDevices) {
			// if (current != null) {
			// current.open();
			// }
			// }
			//
			// for (MidiDevice current : outputDevices) {
			// if (current != null) {
			// current.open();
			// }
			// }
			for (int i = 0; i < inputDevices.size(); i++) {
				inputDevices.get(i).open();
			}
			for (int i = 0; i < outputDevices.size(); i++) {
				outputDevices.get(i).open();
			}

			// connect to control device
			if (controlDevice != null) {
				controlDevice.open();
			}

		} catch (MidiUnavailableException e) {
			e.printStackTrace();
		}

	}

	public void play(Setlist setlist, LinkedBlockingQueue<MyMessage> in, LinkedBlockingQueue<MyMessage> out) {
		inQueue = in;
		outQueue = out;

		// connect to control receiver
		LinkedBlockingQueue<MyMessage> controlQueue = new LinkedBlockingQueue<MyMessage>();
		if (controlDevice != null) {
			controlReceiver = ControlReceiver.newInstance(controlQueue);
			controlTransmitter.setReceiver(controlReceiver);
		}

		// play songs
		for (int i = 0; i < setlist.getNumSongs(); i++) {
			Song s = setlist.getSong(i);

			// update main with current song title
			MyMessage message = new MyMessage(Type.SONG_UPDATE, s.toString());
			try {
				outQueue.put(message);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			// queue used to listen to input receivers. only incoming messages
			LinkedBlockingQueue<MyMessage> playQueue = new LinkedBlockingQueue<MyMessage>();

			// create InputReceiver objects for each required input
			int numInput = s.numInput();
			boolean[] partsDone = new boolean[numInput];
			Arrays.fill(partsDone, false);
			for (int j = 0; j < numInput; j++) {
				InputReceiver recv = InputReceiver.newInstance(s.getInput(j), playQueue);
				inputTransmitters.get(j).setReceiver(recv);
				inputReceivers.set(j, recv);

				// if no control keyboard, send START message to receivers
				// if (controlDevice == null) {
				// recv.notify(new MyMessage(0, Type.START));
				// }
			}

			// connect each outputPart to a Receiver and start their playThread
			int numMIDIOutput = s.numMIDIOutput();
			Thread[] outputThreads = new Thread[numMIDIOutput - 1];
			ArrayList<LinkedBlockingQueue<MyMessage>> outputQueues = new ArrayList<LinkedBlockingQueue<MyMessage>>();
			OutputPart[] outputParts = new OutputPart[numMIDIOutput];
			int currentOutputReceiver = 0;
			for (int j = 0; j < numMIDIOutput; j++) {
				OutputPart current = s.getOutput(j);
				outputParts[j] = current;

				// System.out.println(current.getChannel());
				// start thread if not -1
				if (current.getChannel() != -1) {
					current.setOutput(outputReceivers.get(currentOutputReceiver));
					LinkedBlockingQueue<MyMessage> tq = new LinkedBlockingQueue<MyMessage>();
					outputQueues.add(tq);
					// System.out.println("a");
					Thread t = new Thread(new Runnable() {

						@Override
						public void run() {
							current.play(tq);
							// System.out.println("b");
						}
					});
					t.setName("Output-" + currentOutputReceiver);
					t.start();
					// System.out.println("c");
					outputThreads[currentOutputReceiver] = t;
					currentOutputReceiver++;
				} else {
					// send to TCP Server thread
					System.out.println("Phone output not implemented yet");
				}
			}

			// play song
			// measure all times in units of SECONDS
			boolean songDone = false;
			double partSongTimes[] = new double[numInput];
			Arrays.fill(partSongTimes, 0);
			double partSystemTimes[] = new double[numInput];
			Arrays.fill(partSystemTimes, 0);
			boolean listenForTime[] = new boolean[numInput];
			Arrays.fill(listenForTime, false);
			double currentSongTime = 0;
			double timeMultiplier = 1;
			double partTimeMultipliers[] = new double[numInput];
			Arrays.fill(partTimeMultipliers, 1);
			boolean gotMessage = false;

			// start song if no controller, otherwise wait for controller
			double startTime = 0;
			double previousSystemTime = 0;
			double lastUpdate = 0;
			boolean started = false;
			if (controlReceiver == null) {
				startTime = (double) System.currentTimeMillis() / 1000;
				previousSystemTime = startTime;
				started = true;

				// alert inputReceivers to listen
				for (int j = 0; j < numInput; j++) {
					inputReceivers.get(j).notify(new MyMessage(0, Type.START));
				}

				// send first output
				for (int j = 0; j < numMIDIOutput; j++) {
					if (outputParts[j].getChannel() != -1) {
						outputParts[j].outputFirstEvents();
					}
				}
			}
			System.out.println(startTime);
			while (!songDone) {

				// clock
				double currentSystemTime = 0;
				if (started) {
					currentSystemTime = (double) System.currentTimeMillis() / 1000 - startTime;
					double systemTimeDifference = currentSystemTime;
					if (previousSystemTime != 0) {
						systemTimeDifference = currentSystemTime - previousSystemTime;
					}

					if ((systemTimeDifference * timeMultiplier) > 1.5e-7) {
						currentSongTime += systemTimeDifference * timeMultiplier;
					}

					double timeSinceLastUpdate = currentSystemTime - lastUpdate;

					// update output parts if necessary
					if (timeSinceLastUpdate > 0.0001) {
						MyMessage outputMessage = new MyMessage(Type.TIME_UPDATE, currentSongTime);
						for (LinkedBlockingQueue<MyMessage> c : outputQueues) {
							c.offer(outputMessage);
						}
						lastUpdate = currentSystemTime;
					}

					System.out.println(currentSystemTime + "=>" + currentSongTime);
				}

				// check queues for messages
				MyMessage playMessage = null;
				MyMessage mainMessage = null;
				MyMessage controlMessage = null;

				try {
					playMessage = playQueue.poll(100, TimeUnit.MILLISECONDS);
					mainMessage = inQueue.poll(100, TimeUnit.MILLISECONDS);
					controlMessage = controlQueue.poll(100, TimeUnit.MILLISECONDS);
					// System.out.println("here");
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				if (playMessage != null) {
					gotMessage = true;
					// System.out.println(playMessage.toString());
					// boolean partDone = parseMessage(playMessage);
					boolean partDone = false;
					int channel = playMessage.getChannel();

					// parse message
					switch (playMessage.getType()) {
					case PART_DONE:
						partDone = true;
						break;
					case TIME_UPDATE:
						// process message
						listenForTime[channel] = true;

						double previousPartSongTime = partSongTimes[channel];
						double previousPartSystemTime = partSystemTimes[channel];
						double currentPartSongTime = (Double) playMessage.getData1();
						double nextPartSongTime = (Double) playMessage.getData2();

						double partSongTimeDifference = currentPartSongTime - previousPartSongTime;
						double partSystemTimeDifference = currentSystemTime - previousPartSystemTime;

						if (partSongTimeDifference != 0) {
							double currentPartTimeMultiplier = partSystemTimeDifference / partSongTimeDifference;
							partTimeMultipliers[channel] = currentPartTimeMultiplier;
						}
						// get average of time multipliers and set that to the
						// official time multiplier
						double total = 0;
						double numListen = 0;
						for (int j = 0; j < numInput; j++) {
							if (listenForTime[j]) {
								total += partTimeMultipliers[j];
								numListen++;
							}
						}

						timeMultiplier = total / numListen;
						System.out.println(timeMultiplier);

						// check to see if we should listen for this one when it
						// comes time to calculate next times again
						double timeUntilNextEvent = nextPartSongTime - currentPartSongTime;
						if (timeUntilNextEvent > 10) {
							listenForTime[channel] = false;
						}

						// double previousPartTime = partTimes[channel];
						// double currentPartTime = (Double)
						// playMessage.getData1();
						// double nextPartTime = (Double)
						// playMessage.getData2();

						//
						// double currentPartTime = (Double)
						// playMessage.getData1();
						// double nextPartTime = (Double)
						// playMessage.getData2();
						//
						// // get difference in time between currentPartTime and
						// // the last time we got an update from this part
						// double timeDiff = currentPartTime -
						// partTimes[channel];

						// determine multiplier based upon timeDiff

						// double timeDiff = nextPartTime - partTimes[channel];
						// use what the song originally expected 2 seconds to be
						// if (timeDiff > (2 * timeMultiplier)) {
						// listenForTime[channel] = false;
						// }

						// for (LinkedBlockingQueue<MyMessage> current :
						// outputQueues) {
						// current.offer(playMessage);
						// // System.out.println("here");
						// }
						// for (OutputPart current : outputParts) {
						// long currentTime = (long)playMessage.getData1();
						// current.checkToSend(currentTime, channel);
						// }
						break;
					default:
						System.err.println("MESSAGE TYPE " + playMessage.getType() + " NOT IMPLEMENTED YET");
						break;
					}

					if (partDone) {
						partsDone[channel] = partDone;
					}

					// check if all parts are done
					songDone = true;
					for (boolean current : partsDone) {
						// if current == false
						if (!current) {
							songDone = false;
						}
					}
				}

				// check for message from main
				if (mainMessage != null) {
					gotMessage = true;
					// if (mainMessage.getType() == Type.SYSTEM_EXIT) {
					// songDone = true;
					//
					// // exit while loop
					// break;
					// }

					switch (mainMessage.getType()) {
					case SYSTEM_EXIT:
						songDone = true;
						break;
					case STOP:
						for (int j = 0; j < numInput; j++) {
							InputReceiver current = inputReceivers.get(j);
							current.notify(mainMessage);
						}

						songDone = true;
						for (int j = 0; j < setlist.getNumSongs(); j++) {
							setlist.getSong(j).reset();
						}
						i = setlist.getNumSongs() + 1;
						break;
					default:
						break;
					}
				}

				// check for control message
				if (controlMessage != null) {
					gotMessage = true;
					// parse controlMessage
					int channel = controlMessage.getChannel();
					switch (controlMessage.getType()) {
					case START:
						if (channel == 0 && !started) { // start song
							startTime = (double) System.currentTimeMillis() / 1000;
							previousSystemTime = startTime;
							started = true;

							// alert inputReceivers
							for (int j = 0; j < numInput; j++) {
								inputReceivers.get(j).notify(new MyMessage(0, Type.START));
							}

							// send first output
							for (int j = 0; j < numInput; j++) {
								OutputPart current = outputParts[j];
								if (current.getChannel() != -1) {
									current.outputFirstEvents();
								}
							}
						} else { // start jam
							for (int j = 0; j < numInput; j++) {
								inputReceivers.get(j).notify(new MyMessage(1, Type.START));
							}
						}
						break;
					case STOP:
						if (channel == 0) { // stop song
							songDone = true;

							// notify receivers
							for (int j = 0; j < numInput; j++) {
								inputReceivers.get(j).notify(new MyMessage(0, Type.STOP));
							}

							// get ready to restart current song
							s.reset();
							i--;

							// exit while loop
							break;
						} else { // stop jam
							for (int j = 0; j < numInput; j++) {
								inputReceivers.get(j).notify(new MyMessage(1, Type.STOP));
							}
						}
						break;
					case TIME_UPDATE:
						Double newTime = (double) 0;
						for (int j = 0; j < numInput; j++) {
							Double partTime = inputReceivers.get(j).notify(controlMessage);

							if (partTime != null && partTime > newTime) {
								newTime = partTime;
							}
						}

						// check to see if we need to update parts and clock
						if (newTime != -1) {
							currentSongTime = newTime - startTime;
							previousSystemTime = System.currentTimeMillis() / 1000;
							timeMultiplier = 1;

							for (int j = 0; j < numInput; j++) {
								inputReceivers.get(j).changeTime(newTime);
								partSongTimes[j] = newTime;
								partTimeMultipliers[j] = 1;
								partSystemTimes[j] = previousSystemTime;
							}
						}
						break;
					default:
						System.err.println(controlMessage.toString() + " not implemented in playThread");
						break;
					}

					// make sure not a stop message for the song

					// if (controlMessage.getType() == Type.STOP &&
					// controlMessage.getChannel() == 0) {
					// songDone = true;
					//
					// // notify receivers
					// for (int j = 0; j < numInput; j++) {
					// InputReceiver current = inputReceivers.get(j);
					// current.notify(controlMessage);
					// }
					//
					// // be ready to restart current song
					// s.reset();
					// i--;
					//
					// // exit while loop
					// break;
					// }
					//
					// // forward message to InputReceivers
					// Double newTime = (double) -1;
					// for (int j = 0; j < numInput; j++) {
					// Double partTime =
					// inputReceivers.get(j).notify(controlMessage);
					//
					// if (partTime != null && partTime > newTime) {
					// newTime = partTime;
					// }
					// }
					//
					// // check to see if we need to update parts
					// if (newTime != -1) {
					// for (int j = 0; j < numInput; j++) {
					// inputReceivers.get(j).changeTime(newTime);
					// }
					// }

					// for (InputReceiver current : inputReceivers) {
					// current.notify(controlMessage);
					// }
					// if (controlMessage.getType() == Type.TIME_UPDATE) {
					//
					// // update InputReceivers
					// for (InputReceiver current : inputReceivers) {
					// current.notify(controlMessage);
					// }
					// }
				}

				// change previousSystemTime
				previousSystemTime = currentSongTime;

				if (!gotMessage) {
					gotMessage = false;
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

				// // clock
				// double timeFromStart =
				// ((double)System.currentTimeMillis()/1000) - startTime;
				//
				// currentSongTime = timeFromStart * timeMultiplier;
			}

			// close each receiver now that this song is done
			for (int j = 0; j < numInput; j++) {
				inputTransmitters.get(j).getReceiver().close();
			}
		}
	}

	// parse message from play queue
	// private boolean parseMessage(MyMessage pm) {
	// boolean ret = false;
	//
	// switch (pm.getType()) {
	// case TIME_UPDATE:
	// // update output parts
	// for (OutputPart current : )
	//
	// break;
	// case PART_DONE:
	// ret = true;
	// break;
	// default:
	// break;
	// }
	//
	// return ret;
	//
	// }

}
