package se.nilssonlab.gamsbridge;

import com.gams.api.*;

import java.io.File;

/**
 * A container class for data related to a "session" of GAMS usage
 */
public class GAMSSession
{
	private final GAMSWorkspace workspace;
	private final GAMSJob job;
	private final GAMSDatabase db;
	private final GAMSOptions options;
	private GAMSDatabase resultDB;

	/**
	 * Initiate a GAMS session for a given .gms model file
 	 */
	public GAMSSession(String modelFileName)
	{
		this(new File(modelFileName));
	}

	public GAMSSession(File modelFileName)
	{
		// set working directory to parent directory of the given file
		File workingDirectory = modelFileName.getParentFile();
		GAMSWorkspaceInfo wsInfo  = new GAMSWorkspaceInfo();
		wsInfo.setWorkingDirectory(workingDirectory.getAbsolutePath());
		// create a workspace
		workspace = new GAMSWorkspace(wsInfo);
		// create a GAMS job from the given model file
		job = workspace.addJobFromFile(modelFileName.getAbsolutePath());
		// create an empty database
		db = workspace.addDatabase();
		// set option to include the database into model file when run
		options = workspace.addOptions();
		options.defines("gdxincname", db.getName());
		// results database is initialized when the model is run
		resultDB = null;
	}

	/**
	 * Create a 1-dimensional GAMS set with given elements
	 */
	public GAMSSet createSet(String symbol, String[] elements)
	{
		GAMSSet set = db.addSet(symbol, 1);
		for(String value : elements)
			set.addRecord(value);
		return set;
	}

	/**
	 * Create an arbitrary-dimensional GAMS set with given elements
	 */
	public GAMSSet createSet(String symbol, String[][] elements) {
		GAMSSet set = db.addSet(symbol, elements[0].length);
		for (String[] value : elements)
			set.addRecord(value);
		return set;
	}

	/**
	 * Create a parameter with the given values
	 */
	public GAMSParameter createParameter(String symbol, String[] keys, double[] values)
	{
		GAMSParameter param = db.addParameter(symbol, 1);
		for(int i = 0; i < keys.length; i++) {
			param.addRecord(keys[i]).setValue(values[i]);
		}
		return param;
	}

	public GAMSParameter createParameter(String symbol, String[][] keys, double[] values)
	{
		GAMSParameter param = db.addParameter(symbol, keys[0].length);
		for(int i = 0; i < keys.length; i++) {
			param.addRecord(keys[i]).setValue(values[i]);
		}
		return param;
	}

	/**
	 * Create a variable and set lower/upper bounds and initial values
	 * The variable must not already exist, or a GAMSException is thrown,
	 * so all records for a variable must be created in one call
	 * To fix a variable, set lower = upper = init
	 */
	public void createVariable(String symbol, String[] keys, double[] lower, double[] upper, double[] init)
	{
		String[][] keyArrays = new String[keys.length][1];
		for(int i = 0; i < keys.length; i++)
			keyArrays[i][0] = keys[i];
		createVariable(symbol, keyArrays, lower, upper, init);
	}

	public void createVariable(String symbol, String[][] keys, double[] lower, double[] upper, double[] init)
	{
		// add variable with type "undefined", we define ranges below
		GAMSVariable var = db.addVariable(symbol, keys[0].length, GAMSGlobals.VarType.UNDEFINED_TYPE);
		for(int i = 0; i < keys.length; i++) {
			GAMSVariableRecord record = var.addRecord(keys[i]);
			record.setLower(lower[i]);
			record.setUpper(upper[i]);
			record.setLevel(init[i]);
		}
	}

	/**
	 * Execute the solve statement for the model
	 */
	public void run()
	{
		// run the model
		// this includs the generated database into the model
		job.run(options, db);
		// get the resuts database (with variable values at optimum)
		resultDB = job.OutDB();
	}

	/**
	 * Get current values for specific records of a variable
	 */
	public double[] getVariableValues(String symbol, String[] keys)
	{
		String[][] keyArrays = new String[keys.length][1];
		for(int i = 0; i < keys.length; i++)
			keyArrays[i][0] = keys[i];
		return getVariableValues(symbol, keyArrays);
	}

	public double[] getVariableValues(String symbol, String[][] keys)
	{
		GAMSVariable var = resultDB.getVariable(symbol);
		double[] values = new double[keys.length];
		for(int i = 0; i < keys.length; i++) {
			GAMSVariableRecord rec = var.findRecord(keys[i]);
			values[i] = rec.getLevel();
		}
		return values;
	}

	/**
	 * Get all values from a variable in GAMS iteration order
	 * Use this for scalar variables like the objective function
	 */
	public double[] getVariableValues(String symbol)
	{
		GAMSVariable var = resultDB.getVariable(symbol);
		double[] values = new double[var.getNumberOfRecords()];
		int i = 0;
		for(GAMSVariableRecord varRecord : var) {
			values[i++] = varRecord.getLevel();
		}
		return values;
	}

	/**
	 * Get upper and lower bounds for a variable
	 */
	public double[][] getVariableBounds(String symbol, String[] keys)
	{
		String[][] keyArrays = new String[keys.length][1];
		for(int i = 0; i < keys.length; i++)
			keyArrays[i][0] = keys[i];
		return getVariableBounds(symbol, keyArrays);
	}

	public double[][] getVariableBounds(String symbol, String[][] keys)
	{
		GAMSVariable var = resultDB.getVariable(symbol);
		double[][] bounds = new double[keys.length][2];
		for(int i = 0; i < keys.length; i++) {
			GAMSVariableRecord rec = var.findRecord(keys[i]);
			bounds[i][0] = rec.getLower();
			bounds[i][1] = rec.getUpper();
		}
		return bounds;
	}

	/**
	 * Get all bounds from a variable in GAMS iteration order
	 * Use this for scalar variables like the objective function
	 */
	public double[][] getVariableBounds(String symbol)
	{
		GAMSVariable var = resultDB.getVariable(symbol);
		double[][] bounds = new double[var.getNumberOfRecords()][2];
		int i = 0;
		for(GAMSVariableRecord varRecord : var) {
			bounds[i][0] = varRecord.getLower();
			bounds[i][1] = varRecord.getUpper();
			i++;
		}
		return bounds;
	}

}
