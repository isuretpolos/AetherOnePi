package main

import (
	"flag"
	"fmt"
	"github.com/julienschmidt/httprouter"
	"log"
	"net/http"
)

var (
	data map[string]string
)

/*
	AetherOnePi Server Application
	run "go get" to import all necessary dependencies
*/
func main() {
	fmt.Println("AetherOnePi v3.0 Go")
	flag.Parse()
	data = map[string]string{}

	go startGui()
	startServer()
}

func startGui() {
	panic(http.ListenAndServe(":80", http.FileServer(http.Dir("../src/main/resources/static"))))
}

func startServer() {
	r := httprouter.New()
	r.GET("/entry/:key", show)
	r.GET("/list", show)
	r.PUT("/entry/:key/:value", update)
	err := http.ListenAndServe(":8090", r)
	if err != nil {
		log.Fatal("ListenAndServe:", err)
	}
}

func show(w http.ResponseWriter, r *http.Request, p httprouter.Params) {
	k := p.ByName("key")
	if k == "" {
		fmt.Fprintf(w, "Read list: %v", data)
		return
	}
	fmt.Fprintf(w, "Read entry: data[%s] = %s", k, data[k])
}

func update(w http.ResponseWriter, r *http.Request, p httprouter.Params) {
	k := p.ByName("key")
	v := p.ByName("value")
	data[k] = v
	fmt.Fprintf(w, "Updated: data[%s] = %s", k, data[k])

}
