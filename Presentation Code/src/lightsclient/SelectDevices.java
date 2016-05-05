package lightsclient;

import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;

public class SelectDevices extends Dialog {

	protected Shell shell;
	private String[] inputNames;
	private String[] outputNames;
	private final String[] inputOptions = { "Off", "Input", "Control" };
	private final String[] outputOptions = { "Off", "On" };
	private int[] inputComboSelections;
	private int[] inputChannels;
	private int[] outputComboSelections;
	private int[] outputChannels;
	private ArrayList<Combo> inputCombos;
	private ArrayList<Spinner> inputSpinners;
	private ArrayList<Combo> outputCombos;
	private ArrayList<Spinner> outputSpinners;

	/**
	 * Create the dialog.
	 * 
	 * @param parent
	 * @param style
	 */
	public SelectDevices(Shell parent, int style, String[] inputNames, String[] outputNames) {
		super(parent, style);
		setText("Select Devices");
		this.inputNames = inputNames;
		this.outputNames = outputNames;

		// sort arrays
		Arrays.sort(this.inputNames);
		Arrays.sort(this.outputNames);

		inputComboSelections = new int[inputNames.length];
		inputChannels = new int[inputNames.length];
		outputComboSelections = new int[outputNames.length];
		outputChannels = new int[outputNames.length];

		inputCombos = new ArrayList<Combo>();
		inputSpinners = new ArrayList<Spinner>();
		outputCombos = new ArrayList<Combo>();
		outputSpinners = new ArrayList<Spinner>();

	}

	/**
	 * Open the dialog.
	 * 
	 * @return the result
	 */
	public MidiSelection open() {
		createContents();
		shell.open();
		shell.layout();
		Display display = getParent().getDisplay();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}

		// decrease all channels by 1, MIDI starts at 0, but people read at 1
		// for (int i = 0; i < inputChannels.length; i++) {
		// inputChannels[i];
		// }
		// for (int i = 0; i < outputChannels.length; i++) {
		// outputChannels[i]--;
		// }

		MidiSelection ret = new MidiSelection(inputNames, inputChannels, outputNames, outputChannels);
		return ret;
	}

	/**
	 * Create contents of the dialog.
	 */
	private void createContents() {
		shell = new Shell(getParent(), getStyle());
		// shell.setSize(1, 1);
		shell.setText(getText());
		shell.setLayout(new GridLayout(2, false));

		CTabFolder tabFolder = new CTabFolder(shell, SWT.BORDER);
		tabFolder.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, true, 2, 1));
		tabFolder.setSelectionBackground(
				Display.getCurrent().getSystemColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT));

		CTabItem tbtmInput = new CTabItem(tabFolder, SWT.NONE);
		tbtmInput.setText("Input");

		ScrolledComposite scrolledCompositeInput = new ScrolledComposite(tabFolder,
				SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		scrolledCompositeInput.setExpandVertical(true);
		scrolledCompositeInput.setExpandHorizontal(true);
		tbtmInput.setControl(scrolledCompositeInput);

		Composite compositeInput = new Composite(scrolledCompositeInput, SWT.NONE);
		compositeInput.setLayout(new GridLayout(4, false));
		scrolledCompositeInput.setContent(compositeInput);

		CTabItem tbtmOutput = new CTabItem(tabFolder, SWT.NONE);
		tbtmOutput.setText("Output");

		ScrolledComposite scrolledCompositeOutput = new ScrolledComposite(tabFolder,
				SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		tbtmOutput.setControl(scrolledCompositeOutput);
		scrolledCompositeOutput.setExpandHorizontal(true);
		scrolledCompositeOutput.setExpandVertical(true);

		Composite compositeOutput = new Composite(scrolledCompositeOutput, SWT.NONE);
		compositeOutput.setLayout(new GridLayout(4, false));
		scrolledCompositeOutput.setContent(compositeOutput);
		scrolledCompositeOutput.setMinSize(compositeOutput.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		scrolledCompositeInput.setMinSize(compositeInput.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		scrolledCompositeOutput.setMinSize(compositeOutput.computeSize(SWT.DEFAULT, SWT.DEFAULT));

		Button btnOkay = new Button(shell, SWT.NONE);
		btnOkay.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {

				// populate inputChannels and outputChannels
				for (int i = 0; i < inputNames.length; i++) {
					if (inputSpinners.get(i).isEnabled()) {
						inputChannels[i] = inputSpinners.get(i).getSelection();
					} else {
						inputChannels[i] = -1;
					}
				}
				for (int i = 0; i < outputNames.length; i++) {
					if (outputSpinners.get(i).isEnabled()) {
						outputChannels[i] = outputSpinners.get(i).getSelection();
					} else {
						outputChannels[i] = -1;
					}
				}

				// return to main window
				shell.dispose();
			}
		});
		btnOkay.setText("Okay");
		btnOkay.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, false, false));

		Button btnCancel = new Button(shell, SWT.CANCEL);
		btnCancel.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {

				// set all channels to -1 so that none are connected
				for (int i = 0; i < inputChannels.length; i++) {
					inputChannels[i] = -1;
				}
				for (int i = 0; i < outputChannels.length; i++) {
					outputChannels[i] = -1;
				}

				shell.dispose();
			}
		});
		btnCancel.setText("Cancel");
		btnCancel.setLayoutData(new GridData(SWT.RIGHT, SWT.BOTTOM, false, false));

		// create widgets for each MIDI device
		// input first
		for (int i = 0; i < inputNames.length; i++) {
			Label nameLabel = new Label(compositeInput, SWT.NONE);
			nameLabel.setText(inputNames[i]);

			// create combo
			Combo combo = new Combo(compositeInput, SWT.NONE);
			combo.setItems(inputOptions);
			combo.select(0);
			combo.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					Combo source = (Combo) e.getSource();
					int selection = source.getSelectionIndex();
					int index = inputCombos.indexOf(source);

					inputComboSelections[index] = selection;

					if (selection != 0) {
						inputSpinners.get(index).setEnabled(true);
					} else {
						inputSpinners.get(index).setEnabled(false);
						inputSpinners.get(index).setSelection(0);
					}
				}
			});

			// add to inputCombos
			// when dialog closes, this will be used to get user selections
			inputCombos.add(combo);
			inputComboSelections[i] = -1;

			// create channel label and spinner
			Label channelLabel = new Label(compositeInput, SWT.NONE);
			channelLabel.setText("Channel: ");
			Spinner spinner = new Spinner(compositeInput, SWT.BORDER);
			spinner.setEnabled(false);
			spinner.setMinimum(0);
			spinner.setMaximum(16);

			inputSpinners.add(spinner);
			inputChannels[i] = -1;

			// automatically "connect" if needed to make things easier
			if (inputNames[i].contains("loopMIDI")) {
				// select as input
				inputCombos.get(i).select(1);

				// get channel from end of string
				String channelNumber = inputNames[i].substring(14);
				int channel = new Integer(channelNumber).intValue();

				// select proper channel
				inputSpinners.get(i).setMinimum(1);
				inputSpinners.get(i).setSelection(channel);
				inputSpinners.get(i).setEnabled(true);
			}
		}

		// output
		for (int i = 0; i < outputNames.length; i++) {
			Label nameLabel = new Label(compositeOutput, SWT.NONE);
			nameLabel.setText(outputNames[i]);

			// create combo
			Combo combo = new Combo(compositeOutput, SWT.NONE);
			combo.setItems(outputOptions);
			combo.select(0);

			// add to outputCombos
			// when dialog closes, this will be used to get user selections
			outputCombos.add(combo);
			outputComboSelections[i] = 0;

			// create output label and spinner
			Label outputLabel = new Label(compositeOutput, SWT.NONE);
			outputLabel.setText("Output number: ");
			Spinner spinner = new Spinner(compositeOutput, SWT.BORDER);
			spinner.setEnabled(false);
			spinner.setMinimum(0);

			// add to outputSpinners
			outputSpinners.add(spinner);

			outputChannels[i] = -1;

			// automatically select
			if (outputNames[i].contains("QLC")) {
				// select
				outputCombos.get(i).select(1);
				outputSpinners.get(i).setSelection(1);
				outputSpinners.get(i).setMinimum(1);
				outputSpinners.get(i).setEnabled(true);
			}
		}

		// set sizes
		scrolledCompositeInput.setMinSize(compositeInput.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		scrolledCompositeOutput.setMinSize(compositeOutput.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		shell.setSize(tabFolder.computeSize(SWT.DEFAULT, 256));
	}
}
