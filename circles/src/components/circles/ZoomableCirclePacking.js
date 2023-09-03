import React, {useEffect, useRef} from "react";
import {Inspector, Runtime} from "@observablehq/runtime";
import notebook from "./notebook/5831f55fccfa1c41@187.js";

export function ZoomableCirclePacking() {
    const ref = useRef();

    useEffect(() => {
        const runtime = new Runtime();

        runtime.module(notebook, Inspector.into(ref.current));

        return () => {
            console.log("dispose");
            runtime.dispose();
            ref.current.innerHTML = '';
        }
    }, []);

    return <>
        <div ref={ref}/>
    </>;
}