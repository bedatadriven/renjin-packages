package org.renjin.repo.model;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.io.CharStreams;

public class PackageDescription {

	private ArrayListMultimap<String, String> properties = ArrayListMultimap.create();

	public static class PackageDependency {
		private String name;
    private String version;
		private String versionRange;
    private String versionSpec;

		public PackageDependency(String spec) {
			int versionSpecStart = spec.indexOf('(');
			if(versionSpecStart==-1) {
				name = spec;
				versionRange = "[0,)";
			} else {
				this.name = spec.substring(0, versionSpecStart).trim();

				int versionSpecEnd = spec.indexOf(')', versionSpecStart);
				if(versionSpecEnd == -1) {
					throw new IllegalArgumentException("Unterminated version specification: " + spec);
				}
				versionSpec = spec.substring(versionSpecStart+1, versionSpecEnd).trim();
				if(versionSpec.startsWith(">=")) {
					versionRange = "[" + versionSpec.substring(">=".length()).trim() + ",)";
				} else if(versionSpec.startsWith(">")){
					versionRange = "(" + versionSpec.substring(">".length()).trim() + ",)";
				} else {
					versionRange = versionSpec;
          version = versionSpec;
				}
			}
			if(Strings.isNullOrEmpty(name)) {
				throw new RuntimeException(spec);
			}
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

    /**
     *
     * @return the original version specification, or null if none
     * was provided
     */
    public String getVersionSpec() {
      return versionSpec;
    }

    /**
     * @return Maven-style version range. If none was provided in the description file,
     * it will be [0,)
     */
    public String getVersionRange() {
			return versionRange;
		}

    /**
     *
     * @return the precise version specified in the description file, or null if no single
     * version was specified
     */
    public String getVersion() {
      return version;
    }

    @Override
		public String toString() {
			return (name + "  " + versionRange).trim();
		}


	}

	private static class PackageDependencyParser implements Function<String, PackageDependency> {

		@Override
		public PackageDependency apply(String arg0) {
			if(arg0 == null) {
				throw new RuntimeException();
			}
			return new PackageDependency(arg0);
		}
	}

	public static class Person {
		private String name;
		private String email;

		private Person(String spec) {
			int bracketStart = spec.indexOf('<');
			if(bracketStart == -1) {
				this.name = spec.trim();
			} else {
				try {
					this.name = spec.substring(0, bracketStart).trim();
					int bracketEnd = spec.indexOf('>', bracketStart);
					if(bracketEnd == -1) {
						System.err.println("WARNING: Person '" + spec + "' is missing final '>'");
						this.email = spec.substring(bracketStart+1);
					} else {
						this.email = spec.substring(bracketStart+1, bracketEnd);
					}
				} catch(Exception e) {
					throw new RuntimeException("Error parsing '" + spec + "'");
				}
			}
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getEmail() {
			return email;
		}

		public void setEmail(String email) {
			this.email = email;
		}

		@Override
		public String toString() {
			return name + " <" + email + ">";
		}
	}

	private static class PersonParser implements Function<String, Person> {

		@Override
		public Person apply(String arg0) {
			return new Person(arg0);
		}
	}

	public static PackageDescription fromString(String contents) throws IOException {

		PackageDescription d = new PackageDescription();
		d.properties = ArrayListMultimap.create();

		List<String> lines = CharStreams.readLines(new StringReader(contents));
		String key = null;
		StringBuilder value = new StringBuilder();
		for(String line : lines) {
			if(line.length() > 0) {
				if(Character.isWhitespace(line.codePointAt(0))) {
					if(key == null) {
						throw new IllegalArgumentException("Expected key at line '" + line + "'");
					}
					value.append(" ").append(line.trim());
				} else {
					if(key != null) {
						d.properties.put(key, value.toString());
						value.setLength(0);
					}
					int colon = line.indexOf(':');
					if(colon == -1) {
						throw new IllegalArgumentException("Expected line in format key: value, found '" + line + "'");
					}
					key = line.substring(0, colon);
					value.append(line.substring(colon+1).trim());
				}
			}
		}
		if(key != null) {
			d.properties.put(key, value.toString());
		}
		return d;
	}

	public static PackageDescription fromInputStream(InputStream in) throws IOException {
		return fromString(CharStreams.toString(new InputStreamReader(in)));
	}

	public static PackageDescription fromReader(Reader reader) throws IOException {
		return fromString(CharStreams.toString(reader));
	}

	public String getFirstProperty(String key) {
		if(properties.containsKey(key)) {
			return properties.get(key).iterator().next();
		} else {
			return null;
		}
	}

	public List<String> getProperty(String key) {
		return properties.get(key);
	}

	public boolean hasProperty(String key) {
		return properties.containsKey(key);
	}

	public String getPackage() {
		return getFirstProperty("Package");
	}

	public String getTitle() {
		return getFirstProperty("Title");
	}

	public String getVersion() {
		return getFirstProperty("Version");
	}

	public Iterable<Person> getAuthors() {
		return Iterables.transform(Arrays.asList(getFirstProperty("Author").split("\\s*,\\s*")), new PersonParser());
	}

	public Person getMaintainer() {
		return new Person(getFirstProperty("Maintainer"));
	}

	public String getDescription() {
		return getFirstProperty("Description");
	}

	public Iterable<PackageDependency> getImports() {
		return getPackageDependencyList("Imports");
	}

	public Iterable<PackageDependency> getDepends() {
		return getPackageDependencyList("Depends");
	}

	public Iterable<PackageDependency> getSuggests() {
		return getPackageDependencyList("Suggests");
	}

	private Iterable<PackageDependency> getPackageDependencyList(String property) {
		String list = getFirstProperty(property);
		if(Strings.isNullOrEmpty(list)) {
			return Collections.emptySet();
		} else {
			return Iterables.transform(Arrays.asList(list.split("\\s*,\\s*")), new PackageDependencyParser());
		}
	}

	public String getLicense() {
		return getFirstProperty("License");
	}

	public String getUrl() {
		return getFirstProperty("URL");
	}

	public Iterable<String> getProperties() {
		return properties.keySet();
	}

	public Date getPublicationDate() throws ParseException {
		List<String> dateStrings = properties.get("Date/Publication");
		if(dateStrings.isEmpty()) {
			return null;
		}
		String dateString = dateStrings.get(0);
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return dateFormat.parse(dateString);
	}
}