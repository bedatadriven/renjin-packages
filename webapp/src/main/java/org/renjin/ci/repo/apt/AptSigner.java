

package org.renjin.ci.repo.apt;

import com.google.appengine.tools.cloudstorage.GcsFilename;
import com.google.appengine.tools.cloudstorage.GcsService;
import com.google.appengine.tools.cloudstorage.GcsServiceFactory;
import com.google.appengine.tools.cloudstorage.RetryParams;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.BCPGOutputStream;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.*;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPDigestCalculatorProviderBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;

import java.io.*;
import java.nio.channels.Channels;
import java.security.Security;
import java.util.Iterator;


public class AptSigner {
  static {
    Security.addProvider(new BouncyCastleProvider());
  }

  private static final String PASSPHRASE = "";
  private static final String PROVIDER = "BC";

  private final PGPSecretKey secretKey;
  private final PGPPrivateKey privateKey;

  public AptSigner(PGPSecretKey secretKey) throws PGPException {
    this.secretKey = secretKey;
    this.privateKey = secretKey.extractPrivateKey(
        new JcePBESecretKeyDecryptorBuilder(
          new JcaPGPDigestCalculatorProviderBuilder()
              .setProvider(PROVIDER)
              .build())
            .setProvider(PROVIDER)
            .build(PASSPHRASE.toCharArray()));

  }

  public String getKeyId() {
    return Long.toHexString(secretKey.getKeyID()).toUpperCase();
  }

  public PGPPublicKey getPublicKey() {
    return secretKey.getPublicKey();
  }

  public String sign(String message, boolean inline) throws IOException, PGPException {

    PGPSignatureGenerator signatureGenerator = new PGPSignatureGenerator(
        new JcaPGPContentSignerBuilder(secretKey.getPublicKey().getAlgorithm(), PGPUtil.SHA256)
            .setProvider(PROVIDER));

    PGPSignatureSubpacketGenerator subpacketGenerator = new PGPSignatureSubpacketGenerator();
    signatureGenerator.init(PGPSignature.CANONICAL_TEXT_DOCUMENT, privateKey);

    Iterator<String> it = secretKey.getPublicKey().getUserIDs();
    if (it.hasNext()) {
      subpacketGenerator.setSignerUserID(false, it.next());
      signatureGenerator.setHashedSubpackets(subpacketGenerator.generate());
    }

    ByteArrayOutputStream byteArrayOutput = new ByteArrayOutputStream();
    ArmoredOutputStream armoredOutput = new ArmoredOutputStream(byteArrayOutput);

    if(inline) {
      armoredOutput.beginClearText(PGPUtil.SHA256);
    }

    String[] lines = message.split("\n");
    boolean firstLine = true;
    for (String line : lines) {
      if(!firstLine) {
        signatureGenerator.update((byte) '\r');
        signatureGenerator.update((byte) '\n');
      }
      byte[] bytes = trimTrailingWhitespace(line).getBytes(Charsets.UTF_8);
      signatureGenerator.update(bytes);
      firstLine = false;
      if(inline) {
        armoredOutput.write(bytes);
        armoredOutput.write((byte) '\n');
      }
    }
    if(inline) {
      armoredOutput.endClearText();
    }

    BCPGOutputStream bcpgOut = new BCPGOutputStream(armoredOutput);
    signatureGenerator.generate().encode(bcpgOut);
    armoredOutput.close();

    return new String(byteArrayOutput.toByteArray());
  }

  static String trimTrailingWhitespace(String line) {
    return line.replaceAll("[ \\t\\r\\n]+$", "");
  }

  private static PGPSecretKey findSecretSigningKey(PGPSecretKeyRingCollection pgpSec) {
    Iterator<PGPSecretKeyRing> keyRings = pgpSec.getKeyRings();
    while (keyRings.hasNext()) {
      PGPSecretKeyRing keyRing = keyRings.next();

      Iterator<PGPSecretKey> keys = keyRing.getSecretKeys();
      while (keys.hasNext()) {
        PGPSecretKey key = keys.next();

        if (key.isSigningKey()) {
          return key;
        }
      }
    }

    throw new IllegalStateException("Can't find signing key in key ring.");
  }

  public static AptSigner fromGCS() throws IOException, PGPException {
    return new AptSigner(findSecretSigningKey(readSecretKeyFromGCS()));
  }

  public static AptSigner fromFile(File file) throws IOException, PGPException {
    byte[] bytes = Files.toByteArray(file);

    PGPSecretKeyRingCollection pgpSecretKeyRings = new PGPSecretKeyRingCollection(
        PGPUtil.getDecoderStream(new ByteArrayInputStream(bytes)),
        new JcaKeyFingerprintCalculator());

    return new AptSigner(findSecretSigningKey(pgpSecretKeyRings));
  }

  private static PGPSecretKeyRingCollection readSecretKeyFromGCS() throws IOException, PGPException {
    GcsService gcsService =
        GcsServiceFactory.createGcsService(RetryParams.getDefaultInstance());

    try(InputStream in = Channels.newInputStream(
          gcsService.openPrefetchingReadChannel(new GcsFilename("renjinci-keys", "apt.key"), 0L, 1024*10))) {

      return new PGPSecretKeyRingCollection(
          PGPUtil.getDecoderStream(in),
          new JcaKeyFingerprintCalculator());
    }
  }

  public String getPublicKeyArmored() throws IOException {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    ArmoredOutputStream armoredOutputStream = new ArmoredOutputStream(byteArrayOutputStream);
    getPublicKey().encode(armoredOutputStream);
    armoredOutputStream.close();
    return new String(byteArrayOutputStream.toByteArray(), Charsets.UTF_8);
  }
}