import React, {useState} from 'react';
import * as d3 from "d3";
import "./BarChart.css";
import {Stats} from "./Stats";
import CommitsView from "./CommitsView";
import {useOnce} from "./UseOnce";
import FocusView from "./FocusView";

const config_size = {
    svg_width: 800,
    svg_height: 200,
    plot_margin: {
        left: 30,
        right: 20,
        top: 20,
        bottom: 20,
    },
}

const focus_size = {
    svg_width: 800,
    svg_height: 70,
    plot_margin: {
        left: 30,
        right: 20,
        top: 20,
        bottom: 20,
    },
}

function BarChart() {
    const [data, setData] = useState([]);
    const [updateChart, setUpdateChart] = useState({});

    function convertData(data) {
        return data
            .map(d => {
                return {
                    epoch: new Date((+d.epoch) * 86400000),
                    count: +d.count, // Math.random(), //+d.count,
                    committers: +d.committers,
                    countNormalized: +d.count / +d.committers,
                    files: +d.files
                }
            })
            // .splice(0, 20)
        ;
    }

    function updateCallback(focusedArea) {
        setUpdateChart({focusedArea});
    }

    useOnce( () => {
        d3.csv("commits-per-day.csv").then(data => {
            setData(convertData(data));
        });
    });

    return (
        <>
            <CommitsView
                data={data}
                config={config_size}
                updateChart={updateChart}
            />

            <FocusView
                data={data}
                config={focus_size}
                update={updateCallback}
            />

            {/*<Stats data={data}/>*/}
        </>
    );
}

export default BarChart;