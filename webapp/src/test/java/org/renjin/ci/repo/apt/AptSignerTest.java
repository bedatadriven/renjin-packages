package org.renjin.ci.repo.apt;

import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.openpgp.PGPException;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class AptSignerTest {

  @Test
  public void test() throws IOException, PGPException {
    File keyFile = new File("/home/alex/dev/renjin-ci/apt.key");
    AptSigner signer = AptSigner.fromFile(keyFile);

    String signed = signer.sign("  Hello \nWorld", true);

    System.out.println(signed);

  }

  @Test
  public void trailingWhitespace() {
    assertThat(AptSigner.trimTrailingWhitespace("  Hello  "), equalTo("  Hello"));
  }

  @Test
  public void publicKey() throws IOException, PGPException {
    File keyFile = new File("/home/alex/dev/renjin-ci/apt.key");
    AptSigner signer = AptSigner.fromFile(keyFile);

    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    ArmoredOutputStream armoredOutputStream = new ArmoredOutputStream(byteArrayOutputStream);
    signer.getPublicKey().encode(armoredOutputStream);
    armoredOutputStream.close();

    System.out.println(new String(byteArrayOutputStream.toByteArray()));

  }



}