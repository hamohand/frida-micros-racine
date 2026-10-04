
dg11_hex = '6b7a5c0e5f0e5f0f5f105f2b5f115f425f165f0e0e48414d524f554e3c3ccde5d1e8e6a0140201015f0f0e4d4f48414d4d45443c3ce5cde5cf5f10123130393536303530343030303032313830305f2b0831393536303130335f110e415a415a47413c3cd9d2c7d2e2c95f420a4d3c3cd0e3d13c3c4f2b5f16023c3c'
data = bytes.fromhex(dg11_hex)
idx = 0
while idx < len(data):
    if data[idx] in [0x5F, 0x7F, 0xA0]:
        tag = bytes(data[idx:idx+2])
        idx += 2
    else:
        tag = bytes(data[idx:idx+1])
        idx += 1
    length = data[idx]
    idx += 1
    val = data[idx:idx+length]
    idx += length
    print(f'Tag {tag.hex()} len {length}: {val}')

