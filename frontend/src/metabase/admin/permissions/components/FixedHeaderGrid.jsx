/* eslint-disable react/display-name */

import React, { Component, PropTypes } from "react";

import { Grid, AutoSizer, ScrollSync } from 'react-virtualized'
import 'react-virtualized/styles.css'; // only needs to be imported once

import cx from "classnames";

const SCROLLBAR_SIZE = 10;

const FixedHeaderGrid = ({
    className,

    rowsCount,
    columnsCount,
    renderCell,
    columnWidth,
    rowHeight,

    renderColumnHeader,
    columnHeaderHeight,

    rowHeaderWidth,
    renderRowHeader,

    renderCorner,

    paddingBottom = 25
}) =>
    <div className={cx(className, "relative")}>
        <AutoSizer>
        {({ height, width }) =>
            <ScrollSync>
                {({ clientHeight, clientWidth, onScroll, scrollHeight, scrollLeft, scrollTop, scrollWidth }) =>
                    <div>
                        {/* CORNER */}
                        <div style={{ position: "absolute", top: 0, left: 0, width: rowHeaderWidth, height: columnHeaderHeight, overflow: "hidden" }}>
                            {renderCorner()}
                        </div>
                        {/* COLUMN HEADERS */}
                        <div style={{ position: "absolute", top: 0, left: rowHeaderWidth, height: columnHeaderHeight, overflow: "hidden" }}>
                            <Grid
                                width={width - rowHeaderWidth}
                                height={columnHeaderHeight + SCROLLBAR_SIZE}
                                renderCell={(props) =>
                                    // HACK: offsets the additional height needed to hide the scrollbars
                                    <div style={{ height: columnHeaderHeight, position: "relative" }}>{renderColumnHeader(props)}</div>
                                }
                                columnsCount={columnsCount}
                                rowsCount={1}
                                columnWidth={columnWidth}
                                rowHeight={columnHeaderHeight + SCROLLBAR_SIZE}
                                scrollLeft={scrollLeft}
                            />
                        </div>
                        {/* ROW HEADERS */}
                        <div style={{ position: "absolute", top: columnHeaderHeight, left: 0, width: rowHeaderWidth, overflow: "hidden" }}>
                            <Grid
                                width={rowHeaderWidth + SCROLLBAR_SIZE}
                                height={height - columnHeaderHeight}
                                renderCell={(props) =>
                                    // HACK: pad the bottom with a phantom cell
                                    props.rowIndex >= rowsCount ? <div /> :
                                    // HACK: offsets the additional width needed to hide the scrollbars
                                    <div style={{ width: rowHeaderWidth, position: "relative" }}>{renderRowHeader(props)}</div>
                                }
                                columnsCount={1}
                                rowsCount={rowsCount + 1}
                                columnWidth={rowHeaderWidth + SCROLLBAR_SIZE}
                                rowHeight={(index) => index >= rowsCount ? paddingBottom : rowHeight}
                                scrollTop={scrollTop}
                            />
                        </div>
                        {/* CELLS */}
                        <div style={{ position: "absolute", top: columnHeaderHeight, left: rowHeaderWidth, overflow: "hidden" }}>
                            <Grid
                                width={width - rowHeaderWidth}
                                height={height - columnHeaderHeight}
                                renderCell={(props) =>
                                    // HACK: pad the bottom with a phantom cell
                                    props.rowIndex >= rowsCount ? <div /> :
                                    renderCell(props)
                                }
                                columnsCount={columnsCount}
                                rowsCount={rowsCount + 1}
                                columnWidth={columnWidth}
                                rowHeight={(index) => index >= rowsCount ? paddingBottom : rowHeight}
                                onScroll={onScroll}
                            />
                        </div>
                    </div>
                }
            </ScrollSync>
        }
        </AutoSizer>
    </div>

export default FixedHeaderGrid;
