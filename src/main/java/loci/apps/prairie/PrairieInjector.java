//
// PrairieInjector.java
//

/*
Command line tool to inject OME-XML into Prairie TIFF files.

Copyright (c) 2012, UW-Madison LOCI
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of the UW-Madison LOCI nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.
*/

package loci.apps.prairie;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import loci.common.RandomAccessInputStream;
import loci.common.services.ServiceFactory;
import loci.common.xml.XMLTools;
import loci.formats.ImageReader;
import loci.formats.meta.IMetadata;
import loci.formats.services.OMEXMLService;
import loci.formats.tiff.TiffSaver;
import ome.xml.model.primitives.NonNegativeInteger;

/**
 * Command line tool to inject OME-XML into Prairie TIFF files.
 *
 * <dl><dt><b>Source code:</b></dt>
 * <dd><a href="http://dev.loci.wisc.edu/trac/software/browser/trunk/projects/prairie-ome-tiff/src/main/java/loci/apps/prairie/PrairieInjector.java">Trac</a>,
 * <a href="http://dev.loci.wisc.edu/svn/software/trunk/projects/prairie-ome-tiff/src/main/java/loci/apps/prairie/PrairieInjector.java">SVN</a></dd></dl>
 *
 * @author Kristin Briney
 * @author Curtis Rueden
 */
public class PrairieInjector {

  public static void main(String[] args) throws Exception {
//    System.setProperty("plugins.dir", "C:\\Program Files (x86)\\ImageJ\\plugins");
//    new ImageJ();
//    System.out.println("Instance = " + IJ.getInstance());

//    String infile;
//    String outfile;

//    BufferedReader reader;
//    reader = new BufferedReader(new InputStreamReader(System.in));
//
//    System.out.println("What is name of input file? ");
//    infile = reader.readLine();
//
//    System.out.println("What is name of output file? ");
//    outfile = reader.readLine();

        //String[] myArgs = {infile, outfile};
//      "C:\\users\\Kristin\\Documents\\Dropbox\\LOCI\\DataFromJoe\\TIFF-001\\TIFF-001_Cycle001_CurrentSettings_Ch1_000001.tif",
//      "C:\\users\\Kristin\\Documents\\Dropbox\\LOCI\\DataFromJoe\\TIFF-001\\TestConvert.ome.tif"};
//    ImageInfo.main(myArgs);


    // LAUNCH PROGRAM WITH:
    // java -cp loci_tools.jar;prairie-injector.jar loci.apps.prairie.Injector myPrairieData.xml

//    JFileChooser chooser = new JFileChooser();
    ImageReader reader = new ImageReader();

    ServiceFactory serviceFactory = new ServiceFactory();
    OMEXMLService omexmlService = serviceFactory.getInstance(OMEXMLService.class);

    // create a metadata store, where info is placed
    IMetadata meta = omexmlService.createOMEXMLMetadata();

    // associate that store with the reader
    reader.setMetadataStore(meta);

    // parse the Prairie dataset, populating the metadata store
    // does not read actual image planes
    String pathToPrairieXMLFile = args[0];
    reader.setId(pathToPrairieXMLFile);

    String[] files = reader.getUsedFiles();

    // free resources and close any open files
    // does not wipe out the metadata store
    reader.close();

    // set the TiffData elements to describe the planar ordering
    int tiffDataIndex = 0;
    Map<String, String> uuids = new HashMap<String, String>();
    for (String file : files) {
      if (!isTiff(file)) continue;

      // TODO: populate these values from the current filename
      int c = 0, z = 0, t = 0;

      meta.setTiffDataFirstC(new NonNegativeInteger(c), 0, tiffDataIndex);
      meta.setTiffDataFirstZ(new NonNegativeInteger(z), 0, tiffDataIndex);
      meta.setTiffDataFirstT(new NonNegativeInteger(t), 0, tiffDataIndex);
      meta.setUUIDFileName(file, 0, tiffDataIndex);
      String uuid = UUID.randomUUID().toString();
      meta.setUUIDValue(uuid, 0, tiffDataIndex);
      uuids.put(file, uuid);
      tiffDataIndex++;
    }

    for (String file : files) {
      if (!isTiff(file)) continue;

      // set this TIFF file's UUID to the correct one
      meta.setUUID(uuids.get(file));

      // write out the XML to the TIFF
      String xml = omexmlService.getOMEXML(meta);
      XMLTools.validateXML(xml); // TEMPORARY to make sure it is correct
      RandomAccessInputStream in = new RandomAccessInputStream(file);
      TiffSaver tiffSaver = new TiffSaver(file);
      tiffSaver.overwriteComment(in, xml);
      in.close();
    }


//    JFileChooser chooser = GUITools.buildFileChooser(reader, true);
//    int returnVal = chooser.showOpenDialog(null);

//    ImageConverter.main(myArgs);

//    PrairieReader r = new PrairieReader();
//    r.setId(myFile);
  }

  private static boolean isTiff(String file) {
    return file.toLowerCase().endsWith(".tif") ||
      file.toLowerCase().endsWith(".tiff");
  }

}

