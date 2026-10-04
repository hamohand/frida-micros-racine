from emrtd import readers, eMRTD, _get_mrz_info
import binascii

def check_dg13():
    try:
        r = readers()
        if not r:
            print('No readers')
            return
        c = r[0].createConnection()
        c.connect()
        p = eMRTD(c)
        # We need doc_num, dob, doe for BAC! We don't have a card here.
        pass
    except Exception as e:
        print(e)
check_dg13()
