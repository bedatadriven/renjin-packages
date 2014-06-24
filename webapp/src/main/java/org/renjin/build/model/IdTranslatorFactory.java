package org.renjin.build.model;

import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.TypeUtils;
import com.googlecode.objectify.impl.translate.*;
import org.renjin.build.model.PackageVersionId;

import java.lang.invoke.MethodHandle;

/**
 * Serializes our Id wrapper types as strings
 */
public class IdTranslatorFactory<T> extends ValueTranslatorFactory<T, String> {


  private final Class<T> idType;

  /**
   * @param idType
   */
  public IdTranslatorFactory(Class<T> idType) {
    super(idType);
    this.idType = idType;
  }

  @Override
  protected ValueTranslator createValueTranslator(TypeKey tk, CreateContext ctx, Path path) {

    final MethodHandle constructor = TypeUtils.getConstructor(idType, String.class);

    return new ValueTranslator<T,String>(String.class) {

      @Override
      protected T loadValue(String value, LoadContext ctx, Path path) throws SkipException {
        return TypeUtils.invoke(constructor, value);
      }

      @Override
      protected String saveValue(T value, boolean index, SaveContext ctx, Path path) throws SkipException {
        return value.toString();
      }
    };
  }
}
