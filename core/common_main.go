package main

// Shared by Android and Windows entry points: error output for the parent process and the
// `measure` mode (real latency of subscription servers through the core).

import (
	"fmt"
	"os"
	"strings"
	"sync"
)

func fail(msg string) {
	fmt.Fprintln(os.Stderr, "XKERR "+msg)
	os.Exit(1)
}

func runMeasure() {
	if len(os.Args) < 6 {
		fail("usage: measure sub url timeout idx")
	}
	body, err := os.ReadFile(os.Args[2])
	if err != nil {
		fail(err.Error())
	}
	var to int
	fmt.Sscan(os.Args[4], &to)
	idx := strings.Split(os.Args[5], ",")
	sem := make(chan struct{}, 4)
	var wg sync.WaitGroup
	var out sync.Mutex
	for _, s := range idx {
		var i int
		if _, err := fmt.Sscan(s, &i); err != nil {
			continue
		}
		wg.Add(1)
		sem <- struct{}{}
		go func(i int) {
			defer wg.Done()
			ms := measureSub(body, i, os.Args[3], to)
			<-sem
			out.Lock()
			fmt.Printf("XKMS %d %d\n", i, ms)
			out.Unlock()
		}(i)
	}
	wg.Wait()
}