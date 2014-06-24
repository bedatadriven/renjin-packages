package org.renjin.build;


import org.renjin.build.model.PackageDescription;
import org.renjin.build.model.RPackage;
import org.renjin.build.model.RPackageVersion;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.ParseException;

public class Migrate {


	private final EntityManager em;

	public static void main(String[] args) {

		new Migrate().migrate();
	}

	public Migrate() {
		EntityManagerFactory emf = Persistence.createEntityManagerFactory("renjin-repo");
		em = emf.createEntityManager();
	}

	private void migrate() {

		System.out.println("Scanning for packages...");

		File outputDir = new File("/host/cran");

		em.getTransaction().begin();

		for(File dir : outputDir.listFiles()) {
			if(dir.isDirectory() && !dir.getName().equals("00buildlogs")) {
				try {
					updatePackageVersionMetadata(dir);

				} catch(Exception e) {
					System.err.println("Error building POM for " + dir.getName());
					e.printStackTrace(System.err);
				}
			}
		}

		em.getTransaction().commit();
		em.close();


	}

	private void updatePackageVersionMetadata(File dir) throws IOException {
		FileInputStream fis = new FileInputStream(new File(dir, "DESCRIPTION"));
		PackageDescription description = PackageDescription.fromInputStream(fis);
		fis.close();

		String packageId = "org.renjin.cran:" + description.getPackage();
		RPackage packageEntity = em.find(RPackage.class, packageId);
		if(packageEntity == null) {
			packageEntity = new RPackage();
			packageEntity.setId(packageId);
			packageEntity.setName(description.getPackage());
			em.persist(packageEntity);

			System.out.println(packageId);
		}

		packageEntity.setTitle(description.getTitle());
		packageEntity.setDescription(description.getDescription());

		String versionId = packageId + ":" + description.getVersion();
		RPackageVersion version = em.find(RPackageVersion.class, versionId);
		if(version == null) {
			version = new RPackageVersion();
			version.setId(versionId);
			version.setRPackage(packageEntity);
			version.setVersion(description.getVersion());
			em.persist(version);
		}

		LocAnalyzer locAnalyzer = new LocAnalyzer(dir);
		version.setLoc(locAnalyzer.count());

//		try {
//			version.setPublicationDate(description.getPublicationDate());
//		} catch (ParseException e) {
//			System.err.println("Could not parse publication date:" + description.getProperty("Date/Publication").toString());
//		}
	}
}
