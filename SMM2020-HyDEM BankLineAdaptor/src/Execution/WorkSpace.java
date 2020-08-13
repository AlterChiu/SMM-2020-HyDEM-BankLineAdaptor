package Execution;
import java.util.HashMap;
import java.util.Map;

public class WorkSpace {

	// <WORK FLOW>
	// <------------------------------------------------------------------------>
	/*
	 * 製作完成HyDEM SplitLine(userDefineSplitLine)後 依序執行以下processing
	 * 
	 * 1. SplitPolygonBySplitLine : 將Polygon切分為多段狀
	 * 
	 * 2. ReLinedBankLine : 將段狀Polygon重新組合成"成對"BankLine
	 * 
	 * 3. CheckLevelContinue : 確認各BankLine的高程連續性
	 * 
	 * 4. CreateCenterLine : 建立中心線
	 */

	// workSpace
	public static String workSpace = "E:\\LittleProject\\報告書\\109 - SMM\\測試\\溢堤線更新\\將軍溪\\";
	public static String hydemObjectWorkSpace = workSpace + "溢堤線\\";
	public static String testingWorkSpace = workSpace + "";

	// creating fileName
	public static String splitHydemPolygons = "HyDEM_SplitPolygons.shp"; // HyDEM polygons, which split by splitLine
	public static String splitHydemLines = "HyDEM_SplitLine.shp"; // splitLine, which split polygons from HyDEM
																	// mergedBankLine to
	public static String mergedHydemPolygons = "HyDEM_MergedBankLine.shp";
	public static String bankLineHydem = "HyDEM_BankLine.shp";
	public static String userDefineSplitLine = "userDefine_SplitLine.shp";
	public static String bankLineHydem_Leveling = "HyDEM_BankLineLeveling.csv";
	public static String bankLineHydem_Vertice = "HyDEM_BankLineVertice.shp";

	public static String centerLineHydemPolygons = "HyDEM_CenterLine.shp";
	public static String centerLineFixed = "HyDEM_CenterLine_Fix.shp";

	public static Map<String, String> settingVariables(String[] args) {
		Map<String, String> outputMap = new HashMap<>();
		System.out.println("Input Variables");
		System.out.println("==============================");

		for (int index = 0; index < args.length; index = index + 2) {
			System.out.println(args[index] + "\t\t:\t" + args[index + 1]);
			outputMap.put(args[index], args[index + 1]);
		}

		return outputMap;
	}

}
