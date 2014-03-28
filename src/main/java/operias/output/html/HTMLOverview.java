package operias.output.html;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;


import org.apache.commons.io.IOUtils;

import operias.Configuration;
import operias.cobertura.CoberturaReport;
import operias.diff.DiffFile;
import operias.diff.SourceDiffState;
import operias.report.OperiasFile;
import operias.report.OperiasReport;

public class HTMLOverview {

	/**
	 * List containing the changed files
	 */
	private OperiasReport report;
	
	/**
	 * List containing all the package names
	 */
	private List<String> packageNames;
	
	/**
	 * List of already displayed packages
	 */
	private List<Integer> displayedPackages;
	
	 static class PackageComparator implements Comparator<String>
	 {
	     public int compare(String p1, String p2)
	     {
	    	 int lengthP1 = p1.length() - p1.replace(".", "").length();
	    	 int lengthP2 = p2.length() - p2.replace(".", "").length();
	    	 
	    	 return lengthP1 - lengthP2;
	     }
	 }
	 
	/**
	 * 
	 * @param report
	 * @param packageNames
	 * @throws IOException
	 */
	public HTMLOverview(OperiasReport report, List<String> packageNames) throws IOException {
		this.report = report;
		this.packageNames = packageNames;
		
		Collections.sort(this.packageNames, new PackageComparator());
		
		this.displayedPackages = new LinkedList<Integer>();
		
		Collections.sort(this.packageNames);
		
		File indexHTMLFile = new File(Configuration.getDestinationDirectory() + "/index.html");
		indexHTMLFile.createNewFile();
		
		PrintStream outputStreamHTMLFile = new PrintStream(indexHTMLFile);
		InputStream headerStream = getClass().getResourceAsStream("/html/header.html");
		IOUtils.copy(headerStream, outputStreamHTMLFile);
		
		InputStream legendStream = getClass().getResourceAsStream("/html/overviewlegend.html");
		IOUtils.copy(legendStream, outputStreamHTMLFile);

		outputStreamHTMLFile.println("<div id='mainContent'>");
		
		// ARROW DOWN : &#8595;
		// ARROW UP : &#8593;
		outputStreamHTMLFile.println("<h2>Packages</h2><table class='classOverview'>");
		outputStreamHTMLFile.println("<thead><tr><th>Name</th><th>Line coverage</th><th>Condition coverage</th><th>Source Status</th><tr></thead><tbody>");

		generatePackageOverviewHTML(0, report.getChangedClasses(),outputStreamHTMLFile);
		

		outputStreamHTMLFile.println("</tbody></table>");
		
		
		// Show list of changed test classes
		if (report.getChangedTests().size() > 0) {
			outputStreamHTMLFile.println("<h2>Test Classes</h2><table class='classOverview'>");
			outputStreamHTMLFile.println("<thead><tr><th>Name</th><th>Amount of lines changed</th><tr></thead><tbody>");
	
			for(DiffFile changedTest : report.getChangedTests()) {
				String fileName = changedTest.getFileName(report);
				
				outputStreamHTMLFile.println("<tr >");
				outputStreamHTMLFile.println("<td><a href='"+fileName.replace('/', '.')+".html'>"+fileName+"</a></td>");
				if (changedTest.getSourceState() == SourceDiffState.NEW) {
					outputStreamHTMLFile.println("<td>+"+changedTest.getRevisedLineCount()+" (100%)</td>");
				} else if (changedTest.getSourceState() == SourceDiffState.DELETED) {
					outputStreamHTMLFile.println("<td>-"+changedTest.getOriginalLineCount()+" (100%)</td>");
				} else {
					int changedLineCount = changedTest.getRevisedLineCount() - changedTest.getOriginalLineCount();
					double changedLineCountPercentage = Math.round((double)changedLineCount / (double) changedTest.getOriginalLineCount() * (double)10000) / (double)100;
					outputStreamHTMLFile.println("<td>"+(changedLineCount > 0 ? "+" : "") +changedLineCount+"("+changedLineCountPercentage+"%)</td>");
				}
				outputStreamHTMLFile.println("</tr >");
				
				new HTMLTestView(fileName.replace('/', '.'), changedTest);
				
			}
			
			outputStreamHTMLFile.println("</tbody></table>");
		}
		
		outputStreamHTMLFile.println("</div>");
		
		InputStream footerStream = getClass().getResourceAsStream("/html/footer.html");
		IOUtils.copy(footerStream, outputStreamHTMLFile);
		
		outputStreamHTMLFile.close();
		footerStream.close();
		headerStream.close();
	}

	/**
	 * Display all the packages and its inner classes
	 * @param packageID
	 * @param changedClasses
	 * @param outputStreamHTMLFile
	 */
	private void generatePackageOverviewHTML(int packageID, List<OperiasFile> changedClasses, PrintStream outputStreamHTMLFile) {

		if (packageID >= packageNames.size()) {
			// DONE
			return;
		}
		
		if (displayedPackages.indexOf(packageID) >= 0) {
			// Package already shown somehwere as subpackage, so skip
			generatePackageOverviewHTML(packageID + 1, changedClasses, outputStreamHTMLFile);
			return;
		}
		
		// Generate the HTML for this pacakge
		generateHTML(packageID, changedClasses, outputStreamHTMLFile, 0);
		
		
		// Show next package
		generatePackageOverviewHTML(packageID + 1, changedClasses, outputStreamHTMLFile);

	}
	
	/**
	 * Generate HTML for a specific package
	 * @param packageID
	 * @param changedClasses
	 * @param outputStreamHTMLFile
	 * @param packageLevel The Level of the package, 0 if its a top level package
	 */
	private void generateHTML(int packageID, List<OperiasFile> changedClasses, PrintStream outputStreamHTMLFile, int packageLevel) {
		
		String thisPackageName = this.packageNames.get(packageID);
		CoberturaReport originalReport = report.getOriginalCoverageReport();
		CoberturaReport revisedReport = report.getRevisedCoverageReport();
		
		double revisedPackageLineCoverage = revisedReport.packageExists(thisPackageName) ? revisedReport.getPackage(thisPackageName).getLineRate(): 0.0;
		double revisedPackageConditionCoverage = revisedReport.packageExists(thisPackageName) ? revisedReport.getPackage(thisPackageName).getBranchRate() : 0.0;
		
		double originalPackageLineCoverage = originalReport.packageExists(thisPackageName) ? originalReport.getPackage(thisPackageName).getLineRate() : 0.0;
		double originalPackageConditionCoverage = originalReport.packageExists(thisPackageName) ? originalReport.getPackage(thisPackageName).getBranchRate() : 0.0;
			
		
		int originalPackageRelevantsLinesCount = originalReport.packageExists(thisPackageName) ? originalReport.getPackage(thisPackageName).getRelevantLinesCount() : 0;
		int revisedPackageRelevantsLinesCount = revisedReport.packageExists(thisPackageName) ? revisedReport.getPackage(thisPackageName).getRelevantLinesCount(): 0;
		
	
		
		SourceDiffState packageState = SourceDiffState.CHANGED;
		if (!revisedReport.packageExists(thisPackageName)) {
			packageState = SourceDiffState.DELETED;
		} else if (!originalReport.packageExists(thisPackageName)) {
			packageState = SourceDiffState.NEW;
		}
		
		
		outputStreamHTMLFile.println("<tr class='packageRow level"+packageLevel+" "+(revisedReport.getPackage(thisPackageName) == null ? "deletedOverviewRow" : "")+"' id='Package"+packageID+"'>");
		outputStreamHTMLFile.println("<td>"+thisPackageName+"</td>");
		

		outputStreamHTMLFile.println(generateCoverageBarsHTML(originalPackageLineCoverage, revisedPackageLineCoverage, packageState));
		outputStreamHTMLFile.println(generateCoverageBarsHTML(originalPackageConditionCoverage, revisedPackageConditionCoverage, packageState));
		switch (packageState) {
			case DELETED:
				outputStreamHTMLFile.println("<td>"+ (int)originalPackageRelevantsLinesCount+" (Deleted)</td>");
				break;
			case NEW:
				outputStreamHTMLFile.println("<td>" + (int)revisedPackageRelevantsLinesCount+" (New)</td>");
				break;
			default:
				double packageRelevantLinesSizeChange = revisedPackageRelevantsLinesCount - originalPackageRelevantsLinesCount;
				double packageRelevantLinesSizeChangePercentage = Math.round((double)packageRelevantLinesSizeChange / (double)originalPackageRelevantsLinesCount * (double)10000) / (double)100;	

				outputStreamHTMLFile.println("<td>"+(packageRelevantLinesSizeChange > 0 ? "+" : "") + (int)packageRelevantLinesSizeChange+" ("+packageRelevantLinesSizeChangePercentage+"%)</td>");
				break;
		}	
		
		outputStreamHTMLFile.println("</tr>");
		
		displayedPackages.add(packageID);
		
		
		// Show all classes in the package
		for (int j = 0; j < changedClasses.size(); j++) {
			if (changedClasses.get(j).getPackageName().equals(thisPackageName)) {
				// Class belongs to package
				
				outputStreamHTMLFile.println(generateClassRow(changedClasses.get(j), packageLevel, packageID));
			
			}
		}
		
		// Get all DIRECT subpackages
		for(int j = 0; j < packageNames.size(); j++) {
			if (packageNames.get(j).replace(thisPackageName, "").startsWith(".") && !(displayedPackages.indexOf(j) >= 0)) {
				//Found a DIRECT subpackage
				generateHTML(j, changedClasses, outputStreamHTMLFile, packageLevel + 1);
			}
		}
				
	}
	
	/**
	 * Generate HTML for a sepecific class
	 * @param changedClass
	 * @param packageLevel
	 * @param packageID
	 * @return
	 */
	public String generateClassRow(OperiasFile changedClass, int packageLevel, int packageID) {
		String html = "";

		double revisedClassLineCoverage = (changedClass.getSourceDiff().getSourceState() != SourceDiffState.DELETED) ? changedClass.getRevisedClass().getLineRate() : 0.0;
		double revisedClassConditionCoverage = (changedClass.getSourceDiff().getSourceState() != SourceDiffState.DELETED) ? changedClass.getRevisedClass().getBranchRate() : 0.0;

		double originalClassLineCoverage = (changedClass.getSourceDiff().getSourceState() != SourceDiffState.NEW) ? changedClass.getOriginalClass().getLineRate() : 0.0;
		double originalClassConditionCoverage = (changedClass.getSourceDiff().getSourceState() != SourceDiffState.NEW) ? changedClass.getOriginalClass().getBranchRate() : 0.0;

			
		String[] splittedClassName = changedClass.getClassName().split("\\.");
		String className = splittedClassName[splittedClassName.length - 1];
		
		html += "<tr class=' classRowLevel"+packageLevel+" ClassInPackage"+packageID+" "+(changedClass.getSourceDiff().getSourceState() ==SourceDiffState.DELETED ? "deletedOverviewRow"  : "")+"'>";
		html += "<td><a href='"+changedClass.getClassName()+".html'>"+className+"</a></td>";
		html += generateCoverageBarsHTML(originalClassLineCoverage, revisedClassLineCoverage, changedClass.getSourceDiff().getSourceState());
		html += generateCoverageBarsHTML(originalClassConditionCoverage, revisedClassConditionCoverage, changedClass.getSourceDiff().getSourceState());
		switch (changedClass.getSourceDiff().getSourceState()) {
			case DELETED:
				html += "<td>"+ (int)changedClass.getOriginalClass().getLines().size()+" (Deleted)</td>";
				break;
			case NEW:
				html += "<td>" + (int)changedClass.getRevisedClass().getLines().size()+" (New)</td>";
				break;
			default:
				double classRelevantLinesSizeChange = changedClass.getRevisedClass().getLines().size() - changedClass.getOriginalClass().getLines().size();
				double classRelevantLinesSizeChangePercentage = Math.round((double)classRelevantLinesSizeChange / (double)changedClass.getOriginalClass().getLines().size() * (double)10000) / (double)100;

				html += "<td>"+(classRelevantLinesSizeChange > 0 ? "+" : "") + (int)classRelevantLinesSizeChange+" ("+classRelevantLinesSizeChangePercentage+"%)</td>";
				break;
		}			
		html += "</tr>";
		
		return html;
	}
	
	/**
	 * Generate the columns for the bars
	 * @param originalCoverage
	 * @param revisedCoverage
	 * @param fileState
	 * @return
	 */
	public String generateCoverageBarsHTML(double originalCoverage, double revisedCoverage, SourceDiffState fileState) {
		double coverageChange = Math.round((revisedCoverage - originalCoverage) * (double)10000) / (double)100;
		
		String html = "<td>" + generateCoverageBarHTML(originalCoverage, revisedCoverage, fileState);
		html += "<span class='"+
					// Make the text green or red according to the coverage percentage make it normal if the file was deleted
					((coverageChange > 0) ? "inceasedText" : (coverageChange < 0 && fileState != SourceDiffState.DELETED) ? "decreasedText" : "")+"'>"+
					
					// Plus sign, when the coverage was changed. But not when the file was new or deleted
					((coverageChange > 0 && (fileState == SourceDiffState.CHANGED || fileState == SourceDiffState.SAME)) ? "+" : "")
					
					// The coverage percentage, take absolute value when deleted to remove the - sign
					+(fileState == SourceDiffState.DELETED ? ((int)Math.abs(coverageChange)) : (int)coverageChange)+"%</span>"+ "</td>";
		return html;
	}
	
	
	/**
	 * Get the coverage bar html based on the original and revised coverage number
	 * @param originalCoverage
	 * @param revisedCoverage
	 * @return
	 */
	private String generateCoverageBarHTML(double originalCoverage, double revisedCoverage, SourceDiffState fileState)  {


		String barHTML = "";
		
		switch (fileState) {
		
			case CHANGED:
			case SAME:
				if (originalCoverage > revisedCoverage) {
					barHTML  += "<div class='coverageChangeBar' title='Coverage increased from " + Math.round(originalCoverage * 100) +"% to " + Math.round(revisedCoverage * 100) +"%'>";

					double originalWidth = 100 - Math.floor(originalCoverage * 100);
					double decreasedWidth = Math.ceil(Math.abs(revisedCoverage - originalCoverage) * 100);
					barHTML += "<div class='originalCoverage' style='width:"+(100 - originalWidth - decreasedWidth)+"%'> </div>";
					barHTML += "<div class='decreasedCoverage'  style='width:"+decreasedWidth+"%'> </div>";
					barHTML += "<div class='originalNotCoverage' style='width:"+originalWidth+"%'> </div>";
				} else if (originalCoverage < revisedCoverage) {
					barHTML  += "<div class='coverageChangeBar' title='Coverage decreased from " + Math.round(originalCoverage * 100) +"% to " + Math.round(revisedCoverage * 100) +"%'>";		

					double originalWidth = Math.floor(originalCoverage * 100);
					double increasedWidth = Math.ceil((revisedCoverage - originalCoverage) * 100);
					
					barHTML += "<div class='originalCoverage' style='width:" + originalWidth +"%'> </div>";
					barHTML += "<div class='increasedCoverage' style='width:"+increasedWidth+"%''> </div>";
					barHTML += "<div class='originalNotCoverage' style='width:"+(100 - originalWidth - increasedWidth)+"%'> </div>";
				} else {
					barHTML  += "<div class='coverageChangeBar' title='Coverage stayed the same at " + Math.round(originalCoverage * 100) +"%'>";		

					double originalWidth = Math.floor(originalCoverage * 100);
					
					barHTML += "<div class='originalCoverage' style='width:" + originalWidth +"%'> </div>";
					barHTML += "<div class='originalNotCoverage' style='width:"+(100 - originalWidth)+"%'> </div>";
				}
				break;
			case NEW:
				double revisedCoveredWidth = Math.floor(revisedCoverage * 100);
				
				barHTML  += "<div class='coverageChangeBar' title='Coverage is " + Math.round(revisedCoverage * 100) +"%'>";
				barHTML += "<div class='increasedCoverage' style='width:" + revisedCoveredWidth +"%'> </div>";
				barHTML += "<div class='decreasedCoverage' style='width:"+(100 - revisedCoveredWidth)+"%'> </div>";
				break;
			case DELETED:
				double originalCoveredWidth = Math.ceil(originalCoverage * 100);
				
				barHTML  += "<div class='coverageChangeBar' title='Coverage was " + Math.round(originalCoverage * 100) +"%'>";
				barHTML += "<div class='originalCoverage' style='width:" + originalCoveredWidth +"%'> </div>";
				barHTML += "<div class='originalNotCoverage' style='width:"+(100 - originalCoveredWidth)+"%'> </div>";
				break;
		}
	
		
		return barHTML + "</div>";
	}

	
}
