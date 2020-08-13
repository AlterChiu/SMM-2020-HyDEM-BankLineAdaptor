package Execution;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gdal.ogr.Geometry;

import geo.gdal.IrregularReachBasicControl;
import geo.gdal.SpatialWriter;
import testFolder.SOBEK_OBJECT.SobekBankLine;

public class SOBEK_PairsBankLine {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		// @ input
		// -SOBEK_SpatialFolder (String, folder path)

		// @Output
		// -SOBEK_BankLine (String , spatialFile path)
		// -SOBEK_BankLineError(String, spatialFile path)

		Map<String, String> parameter = WorkSpace.settingVariables(args);

		// setting Variables
		String sobekObjectWorkSpace = parameter.get("-SOBEK_SpatialFolder");
		String pairseBankLine_Error = parameter.get("-SOBEK_BankLineError");
		String pairseBankLine = parameter.get("-SOBEK_BankLine");

		// <================================================>
		// <======== pairs bank line =============================>
		// <================================================>

		/*
		 * @ check bankLine in sobek object to find out is there any bankLine not paired
		 * 
		 * @input : Sbk_Pipe_l.shp
		 * 
		 * @output : SobekBankLine_pairesError.shp or SobekBanLine_pairse.shp
		 * 
		 */
		String sobekBankLineFile = sobekObjectWorkSpace + "Sbk_Pipe_l.shp";
		IrregularReachBasicControl bankLine = new IrregularReachBasicControl(sobekBankLineFile);

		// initial data
		List<Geometry> bankGeoLine = bankLine.getReLinkedEdge();
		List<SobekBankLine> unPariesBankLine = new ArrayList<>();
		for (int index = 0; index < bankGeoLine.size(); index++) {
			SobekBankLine temptBankLine = new SobekBankLine(bankGeoLine.get(index));
			temptBankLine.setID(index);
			unPariesBankLine.add(temptBankLine);
		}

		// detect closest bankLine
		for (int index = 0; index < unPariesBankLine.size() - 1; index++) {
			for (int detectIndex = index + 1; detectIndex < unPariesBankLine.size(); detectIndex++) {
				unPariesBankLine.get(index).getDistance(unPariesBankLine.get(detectIndex));
			}
		}

		// check detection
		List<Geometry> errorGeoList = new ArrayList<>();
		for (int index = 0; index < unPariesBankLine.size() - 1; index++) {
			int linkedIndex = unPariesBankLine.get(index).getLinkedPointID();
			if (linkedIndex < 0) {
				System.out.println("bankLine index " + index + " not pairs");
				errorGeoList.add(unPariesBankLine.get(index).getGeo());
			} else {
				if (unPariesBankLine.get(linkedIndex).getLinkedPointID() != index) {
					System.out.println("bankLine index " + index + " error\t" + index + "\t" + linkedIndex + "\t"
							+ unPariesBankLine.get(linkedIndex).getDistance());
					errorGeoList.add(unPariesBankLine.get(linkedIndex).getGeo());
					errorGeoList.add(unPariesBankLine.get(index).getGeo());
				}
			}
		}
		if (errorGeoList.size() > 0) {
			new SpatialWriter().setGeoList(errorGeoList).saveAsShp(pairseBankLine_Error);
			System.out.println("bankLine paireError, create file " + pairseBankLine_Error);
			System.out.println("Please check SOBEK objedt file, Sbk_Pipe_l.shp");

			// if no error, create pairse bankLine
		} else {
			System.out.println("bankLine pairse complete, create file " + pairseBankLine);

			SpatialWriter sobekBankLine = new SpatialWriter();
			sobekBankLine.addFieldType("ID", "Integer");
			sobekBankLine.addFieldType("LinkedID", "Integer");
			sobekBankLine.addFieldType("Direction", "Integer");

			for (int index = 0; index < unPariesBankLine.size(); index++) {
				Map<String, Object> attrTable = new HashMap<>();
				attrTable.put("ID", unPariesBankLine.get(index).getID());
				attrTable.put("LinkedID", unPariesBankLine.get(index).getLinkedPointID());
				attrTable.put("Direction", unPariesBankLine.get(index).getLinkedDirection());
				sobekBankLine.addFeature(unPariesBankLine.get(index).getGeo(), attrTable);
			}
			sobekBankLine.saveAsShp(pairseBankLine);
		}

	}

}
