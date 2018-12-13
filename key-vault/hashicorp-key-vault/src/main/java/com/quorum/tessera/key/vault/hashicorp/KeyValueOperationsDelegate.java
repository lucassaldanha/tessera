package com.quorum.tessera.key.vault.hashicorp;

import com.quorum.tessera.config.vault.data.HashicorpGetSecretData;
import com.quorum.tessera.config.vault.data.HashicorpSetSecretData;
import org.springframework.vault.core.VaultVersionedKeyValueOperations;
import org.springframework.vault.support.Versioned;

import java.util.Map;

class KeyValueOperationsDelegate {

    private final VaultVersionedKeyValueOperations keyValueOperations;

    KeyValueOperationsDelegate(VaultVersionedKeyValueOperations keyValueOperations) {
        this.keyValueOperations = keyValueOperations;
    }

    Versioned<Map<String, Object>> get(HashicorpGetSecretData getSecretData) {
        return keyValueOperations.get(getSecretData.getSecretName());
    }

    Versioned.Metadata set(HashicorpSetSecretData setSecretData) {
        return keyValueOperations.put(setSecretData.getSecretName(), setSecretData.getNameValuePairs());
    }

}
