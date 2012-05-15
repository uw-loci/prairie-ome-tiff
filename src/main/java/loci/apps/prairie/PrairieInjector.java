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

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import loci.common.RandomAccessInputStream;
import loci.common.services.ServiceFactory;
import loci.formats.ImageReader;
import loci.formats.ome.OMEXMLMetadata;
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
    ImageReader reader = new ImageReader();

    // enable injection of original metadata as structured annotations
    reader.setOriginalMetadataPopulated(true);
    
    ServiceFactory serviceFactory = new ServiceFactory();
    OMEXMLService omexmlService = serviceFactory.getInstance(OMEXMLService.class);

    // create a metadata store, where info is placed
    OMEXMLMetadata meta = omexmlService.createOMEXMLMetadata();
    OMEXMLMetadata meta2 = omexmlService.createOMEXMLMetadata();
    
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

    // define regex for matching Prairie TIFF filenames
    Pattern p = Pattern.compile(".*_Cycle([\\d]+).*_Ch([\\d]+)_([\\d]+).*");

    // set the TiffData elements to describe the planar ordering
    int tiffDataIndex = 0;
    String masterFile = null;
    String masterUuid = null;
    Map<String, String> uuids = new HashMap<String, String>();
    for (String file : files) {
      if (!isTiff(file)) continue;

      // extract CZT values from the current filename
      Matcher m = p.matcher(file);
      if (!m.matches() || m.groupCount() != 3) {
        System.err.println("Warning: " + file + " does not conform to " +
          "Prairie naming convention; skipping.");
        continue;
      }
      
      int t = Integer.parseInt(m.group(1)) - 1;
      int c = Integer.parseInt(m.group(2)) - 1;
      int z = Integer.parseInt(m.group(3)) - 1;
  
      meta.setTiffDataFirstC(new NonNegativeInteger(c), 0, tiffDataIndex);
      meta.setTiffDataFirstZ(new NonNegativeInteger(z), 0, tiffDataIndex);
      meta.setTiffDataFirstT(new NonNegativeInteger(t), 0, tiffDataIndex);  
      
      File f = new File(file);
      String fileName = f.getName();
      meta.setUUIDFileName(fileName, 0, tiffDataIndex);
      String uuid = "urn:uuid:" + UUID.randomUUID().toString();
      meta.setUUIDValue(uuid, 0, tiffDataIndex);
      uuids.put(file, uuid);

      // record master file name and its uuid
      if (tiffDataIndex==0){
        masterFile = file;
        masterUuid = uuid;
      }
      
      tiffDataIndex++;
    }
    
    // set master file information for BinaryOnly files
    meta2.setBinaryOnlyMetadataFile(masterFile);
    meta2.setBinaryOnlyUUID(masterUuid);
    
    int i=0;
    for (String file : files) {
      if (!isTiff(file)) continue;

      // set this TIFF file's UUID to the correct one
      meta.setUUID(uuids.get(file));

      // remove BinData element
      omexmlService.removeBinData(meta);

      // write out the XML to the TIFF
      String xml = omexmlService.getOMEXML(meta2);
      // write full metadata for 1st file and every 10th file after that
      if ((i%10)==0) xml = omexmlService.getOMEXML(meta);
      
      RandomAccessInputStream in = new RandomAccessInputStream(file);
      TiffSaver tiffSaver = new TiffSaver(file);
      tiffSaver.overwriteComment(in, xml);
      in.close();
      
      i++;
    }
  }

  private static boolean isTiff(String file) {
    return file.toLowerCase().endsWith(".tif") ||
      file.toLowerCase().endsWith(".tiff");
  }

}

