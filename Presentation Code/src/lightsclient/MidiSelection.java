package lightsclient;

import java.io.Serializable;

public class MidiSelection implements Serializable {

	private static final long serialVersionUID = -900552899144549785L;
	private int[] inputChannels;
	private int[] outputChannels;
	private String[] inputNames;
	private String[] outputNames;

	public MidiSelection(String[] inputNames, int[] inputChannels, String[] outputNames, int[] outputChannels) {
		this.inputNames = inputNames;
		this.inputChannels = inputChannels;
		this.outputNames = outputNames;
		this.outputChannels = outputChannels;
	}

	public int[] getInputChannels() {
		return inputChannels;
	}

	public String[] getInputNames() {
		return inputNames;
	}

	public int[] getOutputChannels() {
		return outputChannels;
	}

	public String[] getOutputNames() {
		return outputNames;
	}

	public int getInputChannel(String name) {
		// -1 means not found
		int ret = -1;

		for (int i = 0; i < inputNames.length; i++) {
			if (inputNames[i].equals(name)) {
				return inputChannels[i];
			}
		}

		return ret;
	}

	public int getMaxInputChannel() {
		int max = 0;
		for (int i = 0; i < inputChannels.length; i++) {
			if (inputChannels[i] > max) {
				max = inputChannels[i];
			}
		}

		return max;
	}

	public int getOutputChannel(String name) {
		// -1 means not found
		int ret = -1;

		for (int i = 0; i < outputNames.length; i++) {
			if (outputNames[i].equals(name)) {
				return outputChannels[i];
			}
		}

		return ret;
	}

	public int getMaxOutputChannel() {
		int max = 0;
		for (int i = 0; i < outputChannels.length; i++) {
			if (outputChannels[i] > max) {
				max = outputChannels[i];
			}
		}

		return max;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		String ret = "INPUT:\n";

		for (int i = 0; i < inputChannels.length; i++) {
			ret = ret.concat(inputNames[i] + ": ");
			ret = ret.concat(inputChannels[i] + "\n");
		}

		ret = ret.concat("\n");
		ret = ret.concat("OUTPUT:\n");

		for (int i = 0; i < outputChannels.length; i++) {
			ret = ret.concat(outputNames[i] + ": ");
			ret = ret.concat(outputChannels[i] + "\n");
		}

		ret = ret.concat("\n");

		return ret;
	}

	// code found at:
	// http://stackoverflow.com/questions/5837698/converting-any-object-to-a-byte-array-in-java
	// public static byte[] serialize(Object obj) throws IOException {
	// ByteArrayOutputStream b = new ByteArrayOutputStream();
	// ObjectOutputStream o = new ObjectOutputStream(b);
	//
	// o.writeObject(obj);
	// return b.toByteArray();
	// }
	//
	// public static MidiSelection deserialize(byte[] bytes) throws IOException,
	// ClassNotFoundException {
	// ByteArrayInputStream b = new ByteArrayInputStream(bytes);
	// ObjectInputStream o = new ObjectInputStream(b);
	// return (MidiSelection) o.readObject();
	// }
}
