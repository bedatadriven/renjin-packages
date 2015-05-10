package org.renjin.ci.archive;

import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.Ref;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.renjin.ci.datastore.PackageExample;
import org.renjin.ci.datastore.PackageExampleSource;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.pipelines.ForEachPackageVersion;
import org.renjin.parser.RdParser;
import org.renjin.sexp.SEXP;
import org.renjin.sexp.StringVector;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.String.format;


public class ExamplesExtractor extends ForEachPackageVersion {

  private static final Logger LOGGER = Logger.getLogger(ExamplesExtractor.class.getName());


  @Override
  protected void apply(PackageVersionId packageVersionId) {
    Visitor sourceVisitor = new Visitor(packageVersionId);
    sourceVisitor.accept(packageVersionId);

    ObjectifyService.ofy().transactionless().save().entities(sourceVisitor.toSave);
  }

  private static class Visitor extends SourceVisitor {

    private final PackageVersionId packageVersionId;
    private final String expectedPrefix;
    private final List<Object> toSave = new ArrayList<>();

    private Visitor(PackageVersionId packageVersionId) {
      this.packageVersionId = packageVersionId;
      this.expectedPrefix = packageVersionId.getPackageName() + "/man/";
    }

    @Override
    public void visit(TarArchiveEntry entry, InputStream inputStream) throws IOException {
      if(entry.getName().endsWith(".Rd")) {
        LOGGER.info("Found " + entry.getName());
        parseExamples(entry.getName(), inputStream);
      }
    }

    private void parseExamples(String filename, InputStream inputStream) {
      try {
        PackageExampleSource source = extractSource(filename, inputStream);

        PackageExample example = new PackageExample(packageVersionId, formatName(filename));
        example.setSource(Ref.create(source));

        toSave.add(source);
        toSave.add(example);

      } catch(Exception e) {
        LOGGER.log(Level.SEVERE, format("Exception parsing %s in %s: %s",
            filename, packageVersionId, e.getMessage()), e);

        PackageExample example = new PackageExample(packageVersionId, formatName(filename));
        example.setParsingError(e.getMessage());

        toSave.add(example);
      }
    }

    private String formatName(String filename) {
      String name = filename;
      name = stripPrefix(name, expectedPrefix);
      name = stripSuffix(name, ".Rd");

      return name;
    }

    private String stripPrefix(String string, String prefix) {
      if(string.startsWith(prefix)) {
        return string.substring(prefix.length());
      } else {
        return string;
      }
    }

    private String stripSuffix(String string, String suffixToRemove) {
      if(string.endsWith(suffixToRemove)) {
        return string.substring(0, string.length() - suffixToRemove.length());
      } else {
        return string;
      }
    }

    private PackageExampleSource extractSource(String filename, InputStream inputStream) throws IOException {
      InputStreamReader reader = new InputStreamReader(inputStream);
      RdParser parser = new RdParser();
      SEXP rd = parser.R_ParseRd(reader, StringVector.valueOf(filename), false);

      ExamplesParser examples = new ExamplesParser();
      rd.accept(examples);

      return examples.getSource();
    }
  }

}
