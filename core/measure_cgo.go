//go:build cgo

package main

/*
#include <stdlib.h>
*/
import "C"

// XKMeasure: real latency of server `index` of the subscription, through that server. -1 = failed.
//
//export XKMeasure
func XKMeasure(body *C.char, index C.int, url *C.char, timeoutMs C.int) C.int {
	return C.int(measureSub([]byte(C.GoString(body)), int(index), C.GoString(url), int(timeoutMs)))
}
