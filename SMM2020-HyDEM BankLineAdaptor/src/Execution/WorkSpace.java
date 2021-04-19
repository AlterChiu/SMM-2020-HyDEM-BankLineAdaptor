package Execution;

import java.util.HashMap;
import java.util.Map;

import org.gdal.gdal.gdal;
import org.gdal.ogr.DataSource;
import org.gdal.ogr.FeatureDefn;
import org.gdal.ogr.ogr;

public class WorkSpace {
	// <WORK FLOW-SOBEK>
	// <------------------------------------------------------------------------>
	/*
	 * 將建置用用SOBEK專案之SpatialFile吐出到指定資料夾後開始以下流程
	 * 
	 * 1. PairsBankLine : 配對SOBEK專案中的溢堤線，若有特殊狀況會產製Exception檔案
	 * 
	 * 2. PairsBankLineCrossSection : 依照溢堤線配對斷面樁，若有特殊狀況會產製Exception檔案
	 * 
	 * 3. CreateSplitLine : 依照斷面樁產製斷面線，可以設定buffer百分比做調整
	 * 
	 */

	// <WORK FLOW-HyDEM>
	// <------------------------------------------------------------------------>
	/*
	 * 製作完成userDefine_SplitLine後 依序執行以下processing
	 * 
	 * 1. SplitPolygonBySplitLine : 將Polygon切分為多段狀
	 * 
	 * 2. ReLinedBankLine : 將段狀Polygon重新組合成"成對"BankLine
	 * 
	 * 3. CheckLevelContinue : 確認各BankLine的高程連續性
	 * 
	 * 4. CreateCenterLine : 建立中心線
	 * 
	 * 5. FixCenterLine : 修除不合理中心線
	 */

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

	public static Map<String, String> settingVariables() {
		Map<String, String> outputMap = new HashMap<>();
		System.out.println("Input Variables");
		System.out.println("==============================");

		String root = "E:\\LittleProject\\報告書\\109 - SMM\\測試\\溢堤線更新\\港尾溝溪-2021SMM測試\\";

		// SplitPolygonBySplitLine
		outputMap.put("-HyDEM_BankLineFolder", root + "溢堤線\\");
		outputMap.put("-HyDEM_MergedBankLinePolygon", root + "HyDEM_MergedBankLine.shp");
		outputMap.put("-UserDefine_SplitLine", root + "userDefine_SplitLine.shp");
		outputMap.put("-HyDEM_SplitLine", root + "HyDEM_SplitLine_Varified.shp");
		outputMap.put("-HyDEM_SplitBankLinePolygon", root + "HyDEM_SplitedBankLinde.shp");
		
		//ReLinedBankLine
		outputMap.put("-HyDEM_SplitBankLinePolygon", root + "HyDEM_SplitedBankLinde.shp");
		outputMap.put("-HyDEM_BankLine", root + "HyDEM_BankLine.shp");
		
		return outputMap;
	}

}
