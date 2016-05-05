package lightsclient;

import java.util.ArrayList;

public class Song {

	private String title;
	private ArrayList<Part> input;
	private ArrayList<OutputPart> output;
	private ArrayList<Double> measures;
	private ArrayList<Double> parts;

	public Song(String title) {
		this.title = title;

		input = new ArrayList<Part>();
		output = new ArrayList<OutputPart>();
		measures = new ArrayList<Double>();
		parts = new ArrayList<Double>();
	}

	// override toString()
	@Override
	public String toString() {
		return title;
	}

	// give parts what they need
	// output times, measures, parts, etc
	public void process() {

		// give parts measures, parts, and output times
		for (Part p : input) {
			p.addMeasures(measures);
			p.addParts(parts);

			// for (OutputPart op : output) {
			// long[] times = op.getTimes();
			// p.addOutputTimes(times);
			// }

			p.process();
		}
	}

	public void reset() {
		for (Part p : input) {
			p.reset();
		}
		for (OutputPart op : output) {
			op.reset();
		}
	}

	// add input
	public void addInput(Part p) {
		input.add(p);
	}

	// get input
	public Part getInput(int index) {
		return input.get(index);
	}

	// get all input parts
	public ArrayList<Part> getAllInputParts() {
		return input;
	}

	// get number of input parts
	public int numInput() {
		return input.size();
	}

	// add output
	public void addOutput(OutputPart o) {
		output.add(o);
	}

	// get output
	public OutputPart getOutput(int index) {
		return output.get(index);
	}

	// get measures
	public ArrayList<Double> getMeasures() {
		return measures;
	}

	// set measures
	public void setMeasures(ArrayList<Double> m) {
		measures = m;
	}

	// get parts
	public ArrayList<Double> getParts() {
		return parts;
	}

	// set parts
	public void setParts(ArrayList<Double> p) {
		parts = p;
	}

	public int numMIDIOutput() {
		return output.size();
	}
}
