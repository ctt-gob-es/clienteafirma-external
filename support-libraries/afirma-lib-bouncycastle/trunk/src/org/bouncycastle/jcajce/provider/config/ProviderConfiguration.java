package org.bouncycastle.jcajce.provider.config;

import javax.crypto.spec.DHParameterSpec;

import org.bouncycastle.jce.spec.ECParameterSpec;

public interface ProviderConfiguration
{
    ECParameterSpec getEcImplicitlyCa();

    DHParameterSpec getDHDefaultParameters();
}
