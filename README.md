# PSI-CA-Framework
This is the Java implementation of our paper: **Efficient Private Set Intersection Cardinality inthe Reverse Unbalanced Setting Utilizing Hash-Prefix Filter**, we implement the unbalanced PSI-CA of Lv et al.[1] and our proposed reverse unbalanced PSI-CA in the new scenario.

Pamams.java controls the protocol setup and related parameters:
## Protocol type includes:
1. Unbalanced PSI-CA(based on Pohlig-Hellman) : ProtocolEnum.Unbalanced
2. Reverse Unbalanced PSI-CA(based on Pohlig-Hellman) : ProtocolEnum.Reverse
3. Unbalanced PSI-CA(based on ECC) : ProtocolEnum.ECC_Unbalanced
4. Reverse Unbalanced PSI-CA(based on ECC) : ProtocolEnum.ECC_Reverse
## Filter type includes:
1. Bloom Filter : FilterEnum.BloomFilter
2. Cuckoo Filter : FilterEnum.CuckooFilter
## ECC type includes:
1. SM2
2. K283
3. P256
4. secp224r1
5. secp256k1
## Concurrency control: 
THREADS
## Length of Hash Prefix: 
prefix_len
## Notice
pirFilter and preFilter can't both be true, preFilter = true exists privacy leakage, which corresponds to the Pre-Filter Reverse Unbalanced PSI-CA protocol in our paper. On the other hand, pirFilter = true corresponds to the Pre-filter Reverse Unbalanced Protocol with Leakage Resistance.
## Reference
[1] Lv S, Ye J, Yin S, et al. Unbalanced private set intersection cardinality protocol with low communication cost[J]. Future Generation Computer Systems, 2020, 102: 1054-1061.
