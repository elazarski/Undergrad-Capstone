package lightsclient;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;

import javax.sound.midi.InvalidMidiDataException;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

// this class only contains methods for reading files
// it will return necessary data after file(s) have been read
public class MyReader {

	private String home;
	private char fileSeparator;

	// initialize variables
	public MyReader() {
		home = System.getProperty("user.home");
		fileSeparator = File.separatorChar;
	}

	// reads .tar.gz file and returns Song object
	Song readSong(String path) throws IOException, InvalidMidiDataException {
		// initialize Song with title
		int lastIndexOf = path.lastIndexOf(fileSeparator) + 1;
		String title = path.substring(lastIndexOf, path.length() - 7);

		Song ret = new Song(title);

		// code found at
		// http://stackoverflow.com/questions/14402598/extract-a-tar-gz-file-in-java-jsp
		TarArchiveInputStream tarInput = new TarArchiveInputStream(new GZIPInputStream(new FileInputStream(path)));

		BufferedReader br = null;
		TarArchiveEntry currentEntry;
		while ((currentEntry = tarInput.getNextTarEntry()) != null) {
			String fileName = currentEntry.getName();

			// read file into memory
			// code found at:
			// http://www.leveluplunch.com/java/examples/read-file-into-string/
			StringBuffer fileContents = new StringBuffer();
			br = new BufferedReader(new InputStreamReader(tarInput));
			String line;
			while ((line = br.readLine()) != null) {
				if (!line.equals(null)) {
					fileContents.append(line);
					fileContents.append(System.getProperty("line.separator"));
				}
			}

			String lines[] = fileContents.toString().split(System.getProperty("line.separator"));
			if (fileName.contains("i")) {
				// get channel
				int channel = Integer.parseInt(fileName.substring(1));
				Part p = new Part(channel, lines);
				ret.addInput(p);
			} else if (fileName.contains("o")) {
				// get channel
				int channel = Integer.parseInt(fileName.substring(1));

				if (channel == -1) { // Phone output

				}

				OutputPart o = new OutputPart(lines, channel);
				ret.addOutput(o);
			} else {
				// m or p
				if (fileName.equals("m")) {
					// get times
					ArrayList<Double> m = new ArrayList<Double>();
					for (String mLine : lines) {
						if (!mLine.equals("")) {
							String[] elements = mLine.split(" ");
							m.add(Double.parseDouble(elements[0]));
						}
					}

					// add to song
					ret.setMeasures(m);
				} else {
					// p

					// get times
					ArrayList<Double> p = new ArrayList<Double>();
					for (String pLine : lines) {
						if (!pLine.equals("")) {
							String[] elements = pLine.split(" ");
							p.add(Double.parseDouble(elements[0]));
						}
					}

					// add to song
					ret.setParts(p);
				}
			}
		}

		br.close();
		tarInput.close();
		// return song that we have built
		ret.process();
		return ret;
	}

	// read setlist
	// returns Song[] to be passed directly to player
	Setlist readSetlist(String path) throws IOException, InvalidMidiDataException {
		Setlist ret = new Setlist();

		// first read .txt of setlist
		// code modified from:
		// http://stackoverflow.com/questions/16027229/reading-from-a-text-file-and-storing-in-a-string
		ArrayList<String> lines = new ArrayList<String>();
		BufferedReader br = new BufferedReader(new FileReader(path));
		String line = br.readLine();

		while (line != null) {
			lines.add(line);
			line = br.readLine();
		}
		br.close();

		// get paths from lines
		for (int i = 0; i < lines.size(); i++) {
			String p = home + fileSeparator + lines.get(0).replace(':', fileSeparator);
			lines.add(p);

			// remove original line
			lines.remove(0);
		}

		// read songs and return
		for (int i = 0; i < lines.size(); i++) {
			ret.addSong(readSong(lines.get(i)));
		}

		return ret;
	}
}

// open .zip file
// code found at:
// http://stackoverflow.com/questions/15667125/read-content-from-files-which-are-inside-zip-file
// ZipFile zipFile = new ZipFile(path);
// Enumeration<? extends ZipEntry> entries = zipFile.entries();
//
// while (entries.hasMoreElements()) {
// ZipEntry entry = entries.nextElement();
// String name = entry.getName();
//
// if (name.startsWith("i")) {
// InputStream in = zipFile.getInputStream(entry);
//
// int c;
// int index = 0;
// long numTracks = 1;
// long deltaTime = 0;
// long trackLength = 0;
// boolean singleTrack = false;
// boolean multSync = false;
// boolean multASync = false;
// boolean inTrack = false;
// boolean fileHeader = false;
// boolean trackHeader = false;
//
// while ((c = in.read()) != -1) {
//
// // check index
// if (index == 9) {
// // file format, end of file header
// switch (c) {
// case 0: // single track
// singleTrack = true;
// break;
// case 1: // Multiple tracks, synchronous
// multSync = true;
// break;
// case 2: // multiple tracks, asynchronous
// multASync = true;
// break;
// default:
// System.err.println("READING MIDI FILES INCORRECTLY");
// }
//
// } else if (index == 10 && !singleTrack) {
// // number of tracks in file
// int d = in.read();
// numTracks = c + d;
// index++;
// } else if (index == 12) {
// // number of delta-time ticks per quarter note
// int d = in.read();
// deltaTime = c + d;
// index++;
// fileHeader = true;
// } else if (!inTrack && index > 12) {
// // parse track header
// inTrack = true;
// for (int i = 0; i < 3; i++) {
// c = in.read();
// index++;
// }
//
// int d = in.read();
// index++;
// int e = in.read();
// index++;
// int f = in.read();
// index++;
//
// trackLength = c + d + e + f;
// trackHeader = true;
// inTrack = true;
//
// } else if (inTrack && trackHeader) {
// // read whole MIDI event
//
// // time
// long delta_time = c;
// while (c > 0x80) {
// c = in.read();
// delta_time += c;
// index++;
// }
// delta_time += c;
//
// c = in.read();
// index++;
// // c is beginning of MIDI signal, check for NOTE_ON
// if (c >= 0x90 && c <= 0xA0) {
// int command = c;
// c = in.read();
// index++;
// System.out.println(Integer.toHexString(command) + " at " + delta_time + ": "
// + c);
// }
//
// }
//
// index++;
// }
// }
// }
// zipFile.close();
//
// if (name.contains("xml")) {
// // input
// System.out.println(name);
// Sequence seq = MidiSystem.getSequence(f);
// Map m = MidiTools.sortMessagesByTick(seq);
//
// for (Object obj : m.keySet()) {
// Long tick = (Long)obj;
//
// ArrayList<MidiMessage> messages = (ArrayList<MidiMessage>)m.get(tick);
//
// for (MidiMessage msg : messages) {
// if (msg instanceof ShortMessage) {
// ShortMessage sm = (ShortMessage)msg;
//
// if (sm.getCommand() == 0x90) {
// System.out.println("NOTE_ON: " + sm.getData1() + ", " + tick);
// }
// }
// }
// System.out.println();
// } }

/*
 * while (entries.hasMoreElements()) { ZipEntry entry = entries.nextElement();
 * String name = entry.getName();
 * 
 * // check name of current file if (name.startsWith("i")) { // input
 * System.out.println(name); Sequence seq =
 * MidiSystem.getSequence(zipFile.getInputStream(entry)); Map m =
 * MidiTools.sortMessagesByTick(seq);
 * 
 * for (Object obj : m.keySet()) { Long tick = (Long)obj;
 * 
 * ArrayList<MidiMessage> messages = (ArrayList<MidiMessage>)m.get(tick);
 * 
 * for (int i = 0; i < messages.size(); i++) { MidiMessage msg =
 * messages.get(i); if (msg instanceof ShortMessage) { ShortMessage sm =
 * (ShortMessage)msg;
 * 
 * if (sm.getCommand() == ShortMessage.NOTE_ON ) { System.out.println(
 * "NOTE_ON: " + sm.getData1() + ", " + tick); } } } System.out.println(); }
 * 
 * } else if (name.startsWith("o")) { // output
 * 
 * } else { // m or p // get times whether it be m or p // /*long tickLength =
 * sequence.getTickLength(); ret.setTickLength(tickLength);
 * 
 * // parse sequence, extracting the tick of each event ArrayList<Long> ticks =
 * new ArrayList<Long>(); for (Track track : sequence.getTracks()) { for (int i
 * = 0; i < track.size(); i++) { MidiEvent event = track.get(i);
 * 
 * // check if event is a note MidiMessage message = event.getMessage(); if
 * (message instanceof ShortMessage) { ShortMessage sMessage =
 * (ShortMessage)message;
 * 
 * // check if note on here if (sMessage.getCommand() == 0x90) {
 * ticks.add(event.getTick()); } } } }
 * 
 * // check if m or p now if (name.startsWith("p")) { ret.addPartTimes(ticks); }
 * else { ret.addMeasureTimes(ticks); } } }
 */

// close file
// zipFile.close();
